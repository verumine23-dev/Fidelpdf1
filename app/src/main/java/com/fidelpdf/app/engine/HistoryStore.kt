package com.fidelpdf.app.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

data class HistoryEntry(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val score: Float,
    val texts: Int,
    val rules: Int,
    val fills: Int,
    val cutouts: Int,
    val fonts: List<String>,
    val width: Int,
    val height: Int,
    val pdfBytes: Long,
    val source: String
)

/** Stockage local de l'historique : un dossier par scan (photo, aperçus, PDF, informations). */
object HistoryStore {

    private fun root(ctx: Context): File = File(ctx.filesDir, "history").apply { mkdirs() }

    fun dir(ctx: Context, id: Long): File = File(root(ctx), id.toString())

    fun file(ctx: Context, id: Long, name: String): File = File(dir(ctx, id), name)

    fun list(ctx: Context): List<HistoryEntry> {
        val dirs = root(ctx).listFiles() ?: return emptyList()
        val out = ArrayList<HistoryEntry>()
        for (d in dirs) {
            val meta = File(d, "meta.json")
            if (!meta.exists()) continue
            try {
                out.add(parse(JSONObject(meta.readText())))
            } catch (e: Exception) {
                // dossier corrompu : ignoré
            }
        }
        out.sortByDescending { it.createdAt }
        return out
    }

    private fun parse(o: JSONObject): HistoryEntry {
        val fonts = ArrayList<String>()
        val arr = o.optJSONArray("fonts")
        if (arr != null) {
            for (i in 0 until arr.length()) fonts.add(arr.getString(i))
        }
        return HistoryEntry(
            id = o.getLong("id"),
            title = o.getString("title"),
            createdAt = o.getLong("createdAt"),
            score = o.optDouble("score", 0.0).toFloat(),
            texts = o.optInt("texts"),
            rules = o.optInt("rules"),
            fills = o.optInt("fills"),
            cutouts = o.optInt("cutouts"),
            fonts = fonts,
            width = o.optInt("width"),
            height = o.optInt("height"),
            pdfBytes = o.optLong("pdfBytes"),
            source = o.optString("source", "")
        )
    }

    private fun toJson(e: HistoryEntry): JSONObject {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("title", e.title)
        o.put("createdAt", e.createdAt)
        o.put("score", e.score.toDouble())
        o.put("texts", e.texts)
        o.put("rules", e.rules)
        o.put("fills", e.fills)
        o.put("cutouts", e.cutouts)
        val arr = JSONArray()
        for (f in e.fonts) arr.put(f)
        o.put("fonts", arr)
        o.put("width", e.width)
        o.put("height", e.height)
        o.put("pdfBytes", e.pdfBytes)
        o.put("source", e.source)
        return o
    }

    private fun scaled(src: Bitmap, maxW: Int): Bitmap {
        if (src.width <= maxW) return src
        val h = max(1, (src.height * maxW / src.width.toFloat()).roundToInt())
        return Bitmap.createScaledBitmap(src, maxW, h, true)
    }

    private fun writeJpeg(bmp: Bitmap, f: File, quality: Int) {
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }

    /** Enregistre (ou met à jour) un scan. La photo d'origine n'est écrite qu'une fois. */
    fun save(ctx: Context, entry: HistoryEntry, original: Bitmap, preview: Bitmap, pdf: ByteArray, state: String) {
        val d = dir(ctx, entry.id).apply { mkdirs() }
        val orig = File(d, "original.jpg")
        if (!orig.exists()) writeJpeg(original, orig, 93)
        val big = scaled(preview, 1800)
        writeJpeg(big, File(d, "preview.jpg"), 92)
        val thumb = scaled(preview, 420)
        writeJpeg(thumb, File(d, "thumb.jpg"), 85)
        if (big !== preview) big.recycle()
        if (thumb !== preview && thumb !== big) thumb.recycle()
        File(d, "doc.pdf").writeBytes(pdf)
        File(d, "state.json").writeText(state)
        File(d, "meta.json").writeText(toJson(entry).toString())
    }

    fun readState(ctx: Context, id: Long): JSONObject {
        val f = File(dir(ctx, id), "state.json")
        return try {
            if (f.exists()) JSONObject(f.readText()) else JSONObject()
        } catch (e: Exception) {
            JSONObject()
        }
    }

    /** Charge l'image en pleine taille (pour rééditer un scan). */
    fun loadFull(f: File): Bitmap? {
        if (!f.exists()) return null
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeFile(f.absolutePath, opts)
    }

    fun delete(ctx: Context, id: Long) {
        dir(ctx, id).deleteRecursively()
    }

    /** Charge une image réduite (économise la mémoire dans les listes). */
    fun loadBitmap(f: File, maxW: Int): Bitmap? {
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxW) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(f.absolutePath, opts)
    }
}

/** Enregistrement du PDF dans le dossier public « Téléchargements ». */
object Downloads {

    /** Android 10+ : écriture directe via MediaStore, sans permission ni sélecteur de dossier. */
    fun saveToDownloads(ctx: Context, fileName: String, bytes: ByteArray): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw IOException("Utilisez le sélecteur de fichier sur cette version d'Android")
        }
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FidelPDF")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Impossible de créer le fichier dans Téléchargements")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Impossible d'ouvrir le fichier")
            out.use { it.write(bytes) }
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }
}
