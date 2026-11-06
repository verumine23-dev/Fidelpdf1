package com.fidelpdf.app.engine

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/** Les trois modes de lecture. Seul « hors ligne » est disponible pour l'instant. */
enum class OcrMode(val label: String, val sub: String, val available: Boolean) {
    OFFLINE("Hors ligne", "Sur le téléphone, sans internet, confidentiel", true),
    INTELLIGENT("Intelligent", "Gemini : relecture fidèle des lignes (bientôt)", false),
    STANDARD("Standard", "Google Cloud Vision : lecture de documents denses (bientôt)", false);

    companion object {
        fun from(name: String?): OcrMode = entries.firstOrNull { it.name == name && it.available } ?: OFFLINE
    }
}

/** Un mot lu, en pixels de l'image de travail. [conf] : 0..1, ou -1 si le moteur ne la donne pas. */
class OcrWord(val text: String, val rect: Rect, val conf: Float)

interface OcrEngine {
    /** Retourne les lignes de mots, chacune triée de gauche à droite. */
    suspend fun read(work: Bitmap, pix: PixelBuf, progress: (String) -> Unit): List<List<OcrWord>>
}

object OcrEngines {
    fun forMode(mode: OcrMode): OcrEngine = when (mode) {
        // Les modes en ligne seront branchés ici (phases 3 et 4) ; en attendant, lecture locale.
        OcrMode.OFFLINE, OcrMode.INTELLIGENT, OcrMode.STANDARD -> MlKitEngine()
    }
}

/**
 * ML Kit amélioré : lecture de l'image entière, puis relecture agrandie par tuiles,
 * puis relecture isolée des zones d'encre restées sans mot. Tout reste fidèle à l'image :
 * aucune correction orthographique, les mots sont seulement ajoutés ou départagés par confiance.
 */
class MlKitEngine : OcrEngine {

    override suspend fun read(work: Bitmap, pix: PixelBuf, progress: (String) -> Unit): List<List<OcrWord>> {
        val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val lines = ArrayList<MutableList<OcrWord>>()
            for (l in fromText(recognize(rec, work), 0, 0, 1f)) lines.add(l.toMutableList())

            progress("Relecture agrandie par zones")
            val tiled = try {
                tilePass(rec, work)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            } catch (e: OutOfMemoryError) {
                emptyList()
            }
            merge(lines, tiled)

            progress("Recherche des mots oubliés")
            val rescued = try {
                rescue(rec, work, pix, lines)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            } catch (e: OutOfMemoryError) {
                emptyList()
            }
            merge(lines, rescued)

            for (l in lines) l.sortBy { it.rect.left }
            return lines
        } finally {
            rec.close()
        }
    }

    private suspend fun recognize(rec: TextRecognizer, bmp: Bitmap): Text = suspendCancellableCoroutine { cont ->
        rec.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { t -> if (cont.isActive) cont.resume(t) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

    private fun confidenceOf(e: Text.Element): Float = try {
        (e.javaClass.getMethod("getConfidence").invoke(e) as? Float) ?: -1f
    } catch (t: Throwable) {
        -1f
    }

    /** Convertit le résultat ML Kit en mots, en revenant aux pixels de l'image de travail. */
    private fun fromText(t: Text, ox: Int, oy: Int, scale: Float): List<List<OcrWord>> {
        val out = ArrayList<List<OcrWord>>()
        for (block in t.textBlocks) {
            for (line in block.lines) {
                val ws = ArrayList<OcrWord>()
                for (e in line.elements) {
                    val r = e.boundingBox ?: continue
                    if (e.text.isBlank()) continue
                    val rr = Rect(
                        ox + (r.left / scale).toInt(),
                        oy + (r.top / scale).toInt(),
                        ox + (r.right / scale).toInt(),
                        oy + (r.bottom / scale).toInt()
                    )
                    ws.add(OcrWord(TextCleanup.fixWord(e.text), rr, confidenceOf(e)))
                }
                if (ws.isNotEmpty()) out.add(ws)
            }
        }
        return out
    }

    // ------------------------------------------------------------ tuiles agrandies

    private fun starts(total: Int, tile: Int, overlap: Int): List<Int> {
        if (total <= tile + overlap) return listOf(0)
        val step = tile - overlap
        val list = ArrayList<Int>()
        var s = 0
        while (s + tile < total) {
            list.add(s)
            s += step
        }
        list.add(total - tile)
        return list
    }

    private suspend fun tilePass(rec: TextRecognizer, bmp: Bitmap): List<OcrWord> {
        val tile = 1000
        val overlap = 160
        val scale = 1.6f
        val xs = starts(bmp.width, tile, overlap)
        val ys = starts(bmp.height, tile, overlap)
        val out = ArrayList<OcrWord>()
        for (y in ys) {
            for (x in xs) {
                val w = min(tile, bmp.width - x)
                val h = min(tile, bmp.height - y)
                if (w < 40 || h < 40) continue
                val crop = Bitmap.createBitmap(bmp, x, y, w, h)
                val big = Bitmap.createScaledBitmap(crop, (w * scale).toInt(), (h * scale).toInt(), true)
                val res = try {
                    recognize(rec, big)
                } finally {
                    if (big !== crop) big.recycle()
                    if (crop !== bmp) crop.recycle()
                }
                val marginL = if (x == 0) 0 else overlap / 2
                val marginR = if (x + w >= bmp.width) 0 else overlap / 2
                val marginT = if (y == 0) 0 else overlap / 2
                val marginB = if (y + h >= bmp.height) 0 else overlap / 2
                for (line in fromText(res, x, y, scale)) {
                    for (word in line) {
                        val cx = word.rect.centerX()
                        val cy = word.rect.centerY()
                        if (cx < x + marginL || cx > x + w - marginR) continue
                        if (cy < y + marginT || cy > y + h - marginB) continue
                        out.add(word)
                    }
                }
            }
        }
        return out
    }

    // ------------------------------------------------------------ rattrapage des zones d'encre sans mot

    private suspend fun rescue(rec: TextRecognizer, bmp: Bitmap, pix: PixelBuf, lines: List<List<OcrWord>>): List<OcrWord> {
        val s = 3
        val mw = pix.w / s
        val mh = pix.h / s
        if (mw < 20 || mh < 20) return emptyList()
        val mask = BooleanArray(mw * mh)
        for (y in 0 until mh) {
            for (x in 0 until mw) {
                var dark = false
                loop@ for (dy in 0 until s) {
                    for (dx in 0 until s) {
                        if (pix.lum(x * s + dx, y * s + dy) < 150) {
                            dark = true
                            break@loop
                        }
                    }
                }
                mask[y * mw + x] = dark
            }
        }
        // l'encre déjà couverte par un mot est retirée
        for (l in lines) {
            for (w in l) {
                val r = w.rect
                val x0 = max(0, (r.left - 4) / s)
                val x1 = min(mw - 1, (r.right + 4) / s)
                val y0 = max(0, (r.top - 4) / s)
                val y1 = min(mh - 1, (r.bottom + 4) / s)
                for (y in y0..y1) for (x in x0..x1) mask[y * mw + x] = false
            }
        }
        // fermeture horizontale (lettres -> mots -> lignes) puis légère épaisseur verticale
        val closed = BooleanArray(mw * mh)
        for (y in 0 until mh) {
            var last = -1000
            for (x in 0 until mw) {
                if (mask[y * mw + x]) {
                    if (x - last <= 11 && last >= 0) for (k in last..x) closed[y * mw + k] = true
                    closed[y * mw + x] = true
                    last = x
                }
            }
        }
        val grown = closed.copyOf()
        for (y in 1 until mh - 1) for (x in 0 until mw) {
            if (closed[(y - 1) * mw + x] || closed[(y + 1) * mw + x]) grown[y * mw + x] = true
        }

        // composantes connexes
        val seen = BooleanArray(mw * mh)
        val queue = IntArray(mw * mh)
        class Cand(val rect: Rect, val cells: Int)
        val cands = ArrayList<Cand>()
        for (start in 0 until mw * mh) {
            if (!grown[start] || seen[start]) continue
            var qh = 0
            var qt = 0
            queue[qt++] = start
            seen[start] = true
            var minX = mw
            var maxX = 0
            var minY = mh
            var maxY = 0
            var cells = 0
            while (qh < qt) {
                val p = queue[qh++]
                val px = p % mw
                val py = p / mw
                cells++
                if (px < minX) minX = px
                if (px > maxX) maxX = px
                if (py < minY) minY = py
                if (py > maxY) maxY = py
                if (px > 0 && grown[p - 1] && !seen[p - 1]) { seen[p - 1] = true; queue[qt++] = p - 1 }
                if (px < mw - 1 && grown[p + 1] && !seen[p + 1]) { seen[p + 1] = true; queue[qt++] = p + 1 }
                if (py > 0 && grown[p - mw] && !seen[p - mw]) { seen[p - mw] = true; queue[qt++] = p - mw }
                if (py < mh - 1 && grown[p + mw] && !seen[p + mw]) { seen[p + mw] = true; queue[qt++] = p + mw }
            }
            val rect = Rect(minX * s, minY * s, (maxX + 1) * s, (maxY + 1) * s)
            val hh = rect.height()
            val ww = rect.width()
            if (hh < 9 || hh > 110 || ww < 12) continue
            if (ww.toFloat() / hh > 30f) continue // trait de tableau
            if (cells < 6) continue
            cands.add(Cand(rect, cells))
        }
        cands.sortByDescending { it.cells }
        val out = ArrayList<OcrWord>()
        for (c in cands.take(60)) {
            val r = c.rect
            val pad = max(8, r.height() / 2)
            val x0 = max(0, r.left - pad)
            val y0 = max(0, r.top - pad)
            val x1 = min(bmp.width, r.right + pad)
            val y1 = min(bmp.height, r.bottom + pad)
            val w = x1 - x0
            val h = y1 - y0
            if (w < 16 || h < 16) continue
            val scale = (56f / r.height()).coerceIn(1.5f, 4f)
            val crop = Bitmap.createBitmap(bmp, x0, y0, w, h)
            val big = Bitmap.createScaledBitmap(crop, (w * scale).toInt(), (h * scale).toInt(), true)
            val res = try {
                recognize(rec, big)
            } finally {
                if (big !== crop) big.recycle()
                if (crop !== bmp) crop.recycle()
            }
            val zone = Rect(r.left - 4, r.top - 4, r.right + 4, r.bottom + 4)
            for (line in fromText(res, x0, y0, scale)) {
                for (word in line) {
                    if (!zone.contains(word.rect.centerX(), word.rect.centerY())) continue
                    if (word.conf in 0f..0.55f) continue
                    val good = word.text.count { it.isLetterOrDigit() }
                    if (good * 2 < word.text.length) continue
                    out.add(word)
                }
            }
        }
        return out
    }

    // ------------------------------------------------------------ fusion

    private fun overlapRatio(a: Rect, b: Rect): Float {
        val l = max(a.left, b.left)
        val t = max(a.top, b.top)
        val r = min(a.right, b.right)
        val bt = min(a.bottom, b.bottom)
        if (r <= l || bt <= t) return 0f
        val inter = (r - l).toFloat() * (bt - t)
        val minArea = min(a.width().toFloat() * a.height(), b.width().toFloat() * b.height())
        return if (minArea <= 0f) 0f else inter / minArea
    }

    private fun squash(s: String) = s.filter { !it.isWhitespace() }

    /**
     * Ajoute à [lines] les mots de [extras] :
     *  - mot sans équivalent : ajouté à la ligne alignée, ou nouvelle ligne ;
     *  - même mot lu deux fois : on garde la lecture la plus sûre (confiance plus haute d'au moins 0,08) ;
     *  - mot collé en une seule lecture, mais lu en plusieurs mots avec exactement les mêmes lettres : on sépare.
     */
    private fun merge(lines: MutableList<MutableList<OcrWord>>, extras: List<OcrWord>) {
        if (extras.isEmpty()) return
        // index des mots existants
        class Ref(val li: Int, val word: OcrWord)
        val refs = ArrayList<Ref>()
        for ((li, l) in lines.withIndex()) for (w in l) refs.add(Ref(li, w))
        val groups = HashMap<Int, MutableList<OcrWord>>()
        val fresh = ArrayList<OcrWord>()
        for (e in extras) {
            var best = -1
            var bestV = 0.4f
            for ((i, r) in refs.withIndex()) {
                val v = overlapRatio(e.rect, r.word.rect)
                if (v > bestV) {
                    bestV = v
                    best = i
                }
            }
            if (best < 0) fresh.add(e) else groups.getOrPut(best) { ArrayList() }.add(e)
        }
        val replacements = HashMap<OcrWord, List<OcrWord>>()
        for ((i, g) in groups) {
            val old = refs[i].word
            if (g.size == 1) {
                val e = g[0]
                val ratio = e.text.length.toFloat() / max(1, old.text.length)
                if (e.conf >= 0f && old.conf >= 0f && e.conf > old.conf + 0.08f && ratio in 0.7f..1.4f) {
                    val rect = if (e.text.length == old.text.length) old.rect else e.rect
                    replacements[old] = listOf(OcrWord(e.text, rect, e.conf))
                }
            } else {
                val sorted = g.sortedBy { it.rect.left }
                if (squash(sorted.joinToString("") { it.text }) == squash(old.text)) {
                    replacements[old] = sorted
                }
            }
        }
        if (replacements.isNotEmpty()) {
            for (l in lines) {
                var k = 0
                while (k < l.size) {
                    val rep = replacements[l[k]]
                    if (rep != null) {
                        l.removeAt(k)
                        l.addAll(k, rep)
                        k += rep.size
                    } else {
                        k++
                    }
                }
            }
        }
        // mots nouveaux (doublons entre eux écartés)
        val added = ArrayList<OcrWord>()
        for (e in fresh.sortedByDescending { max(0f, it.conf) }) {
            if (added.any { overlapRatio(e.rect, it.rect) > 0.4f }) continue
            added.add(e)
        }
        for (e in added) {
            var target = -1
            val h = e.rect.height()
            for ((li, l) in lines.withIndex()) {
                if (l.isEmpty()) continue
                var best: OcrWord? = null
                var dBest = Int.MAX_VALUE
                for (w in l) {
                    val vo = min(w.rect.bottom, e.rect.bottom) - max(w.rect.top, e.rect.top)
                    if (vo < 0.5f * min(w.rect.height(), h)) continue
                    val gap = if (e.rect.left > w.rect.right) e.rect.left - w.rect.right
                    else if (w.rect.left > e.rect.right) w.rect.left - e.rect.right
                    else 0
                    if (gap < dBest) {
                        dBest = gap
                        best = w
                    }
                }
                if (best != null && dBest <= 1.2f * max(h, best.rect.height())) {
                    target = li
                    break
                }
            }
            if (target >= 0) lines[target].add(e) else lines.add(arrayListOf(e))
        }
        for (l in lines) l.sortBy { it.rect.left }
    }
}
