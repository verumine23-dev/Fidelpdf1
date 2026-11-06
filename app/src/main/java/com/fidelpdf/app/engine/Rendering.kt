package com.fidelpdf.app.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Fonts {
    private val cache = HashMap<String, Typeface>()

    fun get(assets: AssetManager, f: FontChoice): Typeface {
        val style = when {
            f.bold && f.italic -> "BoldItalic"
            f.bold -> "Bold"
            f.italic -> "Italic"
            else -> "Regular"
        }
        val file = "${f.family.prefix}-$style.ttf"
        synchronized(cache) {
            val hit = cache[file]
            if (hit != null) return hit
            val tf = Typeface.createFromAsset(assets, "fonts/$file")
            cache[file] = tf
            return tf
        }
    }
}

/** Calage d'un texte dans sa boîte d'encre : taille et étirement pour coller au pixel près. */
object TextFit {
    class Placement(val x: Float, val baseline: Float, val size: Float, val scaleX: Float)

    fun place(text: String, box: RectF, tf: Typeface, paint: Paint): Placement {
        paint.typeface = tf
        paint.textSize = 100f
        paint.textScaleX = 1f
        val b = Rect()
        paint.getTextBounds(text, 0, text.length, b)
        val bw = max(1, b.width()).toFloat()
        val bh = max(1, b.height()).toFloat()
        val boxH = max(1f, box.height())
        val boxW = max(1f, box.width())
        val size = (100f * boxH / bh).coerceIn(boxH * 0.5f, boxH * 3f)
        val k = size / 100f
        val sx = (boxW / (bw * k)).coerceIn(0.5f, 2f)
        val x = box.left - b.left * k * sx
        val baseline = box.top - b.top * k
        return Placement(x, baseline, size, sx)
    }

    /** Taille et ligne de base imposées (harmonisation) ; seule la largeur est ajustée à la boîte. */
    fun placeFixed(text: String, box: RectF, tf: Typeface, paint: Paint, size: Float, baseline: Float): Placement {
        paint.typeface = tf
        paint.textSize = size
        paint.textScaleX = 1f
        val b = Rect()
        paint.getTextBounds(text, 0, text.length, b)
        val bw = max(1, b.width()).toFloat()
        val sx = (max(1f, box.width()) / bw).coerceIn(0.5f, 2f)
        val x = box.left - b.left * sx
        return Placement(x, baseline, size, sx)
    }

    /** Texte modifié : on garde la taille, l'étirement et l'origine du texte d'origine. */
    fun placeEdited(
        original: String,
        box: RectF,
        tf: Typeface,
        paint: Paint,
        fixedSize: Float,
        fixedBaseline: Float
    ): Placement {
        val base = if (original.isBlank()) "x" else original
        return if (fixedSize > 0f) placeFixed(base, box, tf, paint, fixedSize, fixedBaseline)
        else place(base, box, tf, paint)
    }
}

object Renderer {
    /** Dessine le document en coordonnées « pixels image » (le canvas doit être mis à l'échelle avant). */
    fun draw(canvas: Canvas, model: DocModel, assets: AssetManager) {
        canvas.drawColor(model.paperColor)

        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        for (f in model.fills) {
            fillPaint.color = f.color
            canvas.drawRect(f.rect, fillPaint)
        }

        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }
        for (r in model.rules) {
            linePaint.color = r.color
            linePaint.strokeWidth = max(0.8f, r.thickness)
            canvas.drawLine(r.x1, r.y1, r.x2, r.y2, linePaint)
        }

        val tp = Paint(Paint.ANTI_ALIAS_FLAG)
        for (t in model.texts) {
            if (t.text.isBlank()) continue
            val tf = Fonts.get(assets, t.font)
            val p = if (t.text == t.original) {
                if (t.fixedSize > 0f) {
                    TextFit.placeFixed(t.text, t.box, tf, tp, t.fixedSize, t.fixedBaseline)
                } else {
                    TextFit.place(t.text, t.box, tf, tp)
                }
            } else {
                TextFit.placeEdited(t.original, t.box, tf, tp, t.fixedSize, t.fixedBaseline)
            }
            tp.typeface = tf
            tp.textSize = p.size
            tp.textScaleX = p.scaleX
            tp.color = t.color
            if (t.bold > 0.05f) {
                tp.style = Paint.Style.FILL_AND_STROKE
                tp.strokeWidth = t.bold
                tp.strokeJoin = Paint.Join.ROUND
            } else {
                tp.style = Paint.Style.FILL
                tp.strokeWidth = 0f
            }
            canvas.drawText(t.text, p.x, p.baseline, tp)
        }

        val bp = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        for (c in model.cutouts) {
            canvas.drawBitmap(c.bitmap, null, c.rect, bp)
        }
    }

    fun renderBitmap(model: DocModel, assets: AssetManager, maxW: Int = 2400): Bitmap {
        val s = min(1f, maxW / model.width.toFloat())
        val w = max(1, (model.width * s).roundToInt())
        val h = max(1, (model.height * s).roundToInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(s, s)
        draw(c, model, assets)
        return bmp
    }
}

object PdfExport {
    /** Écrit un PDF vectoriel : texte réel, polices embarquées, traits et aplats vectoriels. */
    fun write(model: DocModel, assets: AssetManager, out: OutputStream) {
        val pageW = 595
        val scale = pageW / model.width.toFloat()
        val pageH = max(1, (model.height * scale).roundToInt())
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
            page.canvas.scale(scale, scale)
            Renderer.draw(page.canvas, model, assets)
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }
}
