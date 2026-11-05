package com.fidelpdf.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Détourage : on découpe la zone choisie, on supprime le fond du papier et on garde
 * une image transparente qui sera recollée à la position d'origine.
 */
object Cutouts {

    fun extract(pix: PixelBuf, rectIn: RectF, kind: CutoutKind, id: Int): Cutout? {
        val x0 = rectIn.left.toInt().coerceIn(0, pix.w - 1)
        val y0 = rectIn.top.toInt().coerceIn(0, pix.h - 1)
        val x1 = rectIn.right.toInt().coerceIn(x0, pix.w)
        val y1 = rectIn.bottom.toInt().coerceIn(y0, pix.h)
        val w = x1 - x0
        val h = y1 - y0
        if (w < 8 || h < 8) return null
        val colors = IntArray(w * h)
        for (y in 0 until h) {
            System.arraycopy(pix.px, (y0 + y) * pix.w + x0, colors, y * w, w)
        }
        return when (kind) {
            CutoutKind.STAMP, CutoutKind.SIGNATURE -> ink(colors, w, h, x0, y0, kind, id)
            CutoutKind.LOGO -> logo(colors, w, h, x0, y0, id)
            CutoutKind.PHOTO -> photo(colors, w, h, x0, y0, id)
        }
    }

    // --- Tampons et signatures : l'intensité de l'encre devient la transparence.
    private fun ink(colors: IntArray, w: Int, h: Int, x0: Int, y0: Int, kind: CutoutKind, id: Int): Cutout? {
        val n = w * h
        val paper = Paper.estimate(colors, 1)
        val d = IntArray(n)
        val hist = IntArray(256)
        for (i in 0 until n) {
            d[i] = min(255, Colors.dist(colors[i], paper))
            hist[d[i]]++
        }
        // « Pic » d'encre : valeur atteinte par les 0,3 % de pixels les plus marqués
        val topN = max(5, (n * 0.003f).toInt())
        var acc = 0
        var peak = 0
        for (v in 255 downTo 0) {
            acc += hist[v]
            if (acc >= topN) {
                peak = v
                break
            }
        }
        if (peak < 30) return null

        // Couleur de l'encre = moyenne des pixels les plus foncés / saturés
        var cutoff = (peak * 0.85f).toInt()
        var sr = 0L
        var sg = 0L
        var sb = 0L
        var cnt = 0
        for (pass in 0 until 2) {
            sr = 0; sg = 0; sb = 0; cnt = 0
            for (i in 0 until n) {
                if (d[i] >= cutoff) {
                    sr += Colors.red(colors[i])
                    sg += Colors.green(colors[i])
                    sb += Colors.blue(colors[i])
                    cnt++
                }
            }
            if (cnt >= 3) break
            cutoff = (peak * 0.6f).toInt()
        }
        if (cnt == 0) return null
        val inkColor = Colors.rgb((sr / cnt).toInt(), (sg / cnt).toInt(), (sb / cnt).toInt())

        val pr = Colors.red(paper).toFloat()
        val pg = Colors.green(paper).toFloat()
        val pb = Colors.blue(paper).toFloat()
        val ur = pr - Colors.red(inkColor)
        val ug = pg - Colors.green(inkColor)
        val ub = pb - Colors.blue(inkColor)
        val uu = ur * ur + ug * ug + ub * ub
        if (uu < 900f) return null
        val tolRes = 70f

        val alpha = IntArray(n)
        for (i in 0 until n) {
            val c = colors[i]
            val vr = pr - Colors.red(c)
            val vg = pg - Colors.green(c)
            val vb = pb - Colors.blue(c)
            val a = ((vr * ur + vg * ug + vb * ub) / uu).coerceIn(0f, 1f)
            val rr = vr - a * ur
            val rg = vg - a * ug
            val rb = vb - a * ub
            val res = sqrt(rr * rr + rg * rg + rb * rb)
            // Un pixel qui s'écarte de la teinte de l'encre (ex. texte noir sous un tampon bleu) n'est pas du tampon.
            val f = ((tolRes + 30f - res) / 30f).coerceIn(0f, 1f)
            var al = a * f
            al = if (al < 0.08f) 0f else (al - 0.08f) / 0.92f
            alpha[i] = (al * 255f).toInt().coerceIn(0, 255)
        }

        return finish(alpha, inkColor, null, w, h, x0, y0, kind, id, inkColor)
    }

    // --- Logos : on garde les couleurs, on retire le fond depuis les bords.
    private fun logo(colors: IntArray, w: Int, h: Int, x0: Int, y0: Int, id: Int): Cutout? {
        val n = w * h
        // Couleur du papier = couleur dominante de l'anneau extérieur
        val ring = ArrayList<Int>()
        for (x in 0 until w) {
            ring.add(colors[x])
            ring.add(colors[(h - 1) * w + x])
        }
        for (y in 0 until h) {
            ring.add(colors[y * w])
            ring.add(colors[y * w + w - 1])
        }
        val paper = Paper.estimate(ring.toIntArray(), 1)

        val tol = 38
        val soft = 90
        val alpha = IntArray(n) { 255 }
        val visited = BooleanArray(n)
        val queue = IntArray(n)
        var head = 0
        var tail = 0
        fun push(i: Int) {
            if (!visited[i] && Colors.dist(colors[i], paper) < tol) {
                visited[i] = true
                queue[tail++] = i
            }
        }
        for (x in 0 until w) {
            push(x)
            push((h - 1) * w + x)
        }
        for (y in 0 until h) {
            push(y * w)
            push(y * w + w - 1)
        }
        while (head < tail) {
            val cur = queue[head++]
            alpha[cur] = 0
            val cx = cur % w
            val cy = cur / w
            if (cx > 0) push(cur - 1)
            if (cx < w - 1) push(cur + 1)
            if (cy > 0) push(cur - w)
            if (cy < h - 1) push(cur + w)
        }
        // Bords adoucis : pixels voisins du fond supprimé, partiellement transparents
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (alpha[i] == 0) continue
                val nearBg = (x > 0 && alpha[i - 1] == 0) || (x < w - 1 && alpha[i + 1] == 0) ||
                    (y > 0 && alpha[i - w] == 0) || (y < h - 1 && alpha[i + w] == 0)
                if (nearBg) {
                    val dd = Colors.dist(colors[i], paper)
                    if (dd < soft) alpha[i] = ((dd - tol).toFloat() / (soft - tol) * 255f).toInt().coerceIn(0, 255)
                }
            }
        }
        return finish(alpha, 0, colors, w, h, x0, y0, CutoutKind.LOGO, id, Color.BLACK)
    }

    // --- Photos : recadrage simple, sans suppression de fond.
    private fun photo(colors: IntArray, w: Int, h: Int, x0: Int, y0: Int, id: Int): Cutout? {
        val alpha = IntArray(w * h) { 255 }
        return finish(alpha, 0, colors, w, h, x0, y0, CutoutKind.PHOTO, id, Color.BLACK)
    }

    /** Assemble le bitmap ARGB, rogné au contenu visible. [flat] = couleur unie (encre), sinon [colors]. */
    private fun finish(
        alpha: IntArray,
        flat: Int,
        colors: IntArray?,
        w: Int,
        h: Int,
        x0: Int,
        y0: Int,
        kind: CutoutKind,
        id: Int,
        inkColor: Int
    ): Cutout? {
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (alpha[y * w + x] > 24) {
                    minX = min(minX, x)
                    maxX = max(maxX, x)
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                }
            }
        }
        if (maxX < 0) return null
        val margin = 2
        minX = max(0, minX - margin)
        minY = max(0, minY - margin)
        maxX = min(w - 1, maxX + margin)
        maxY = min(h - 1, maxY + margin)
        val ow = maxX - minX + 1
        val oh = maxY - minY + 1
        val out = IntArray(ow * oh)
        for (y in 0 until oh) {
            for (x in 0 until ow) {
                val si = (minY + y) * w + (minX + x)
                val a = alpha[si]
                val base = if (colors != null) colors[si] else flat
                out[y * ow + x] = (a shl 24) or (base and 0x00FFFFFF)
            }
        }
        val bmp = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, ow, 0, 0, ow, oh)
        val rect = RectF(
            (x0 + minX).toFloat(),
            (y0 + minY).toFloat(),
            (x0 + maxX + 1).toFloat(),
            (y0 + maxY + 1).toFloat()
        )
        return Cutout(id, kind, rect, bmp, inkColor)
    }
}

object ModelOps {

    private fun fractionInside(a: RectF, r: RectF): Float {
        val ix = max(0f, min(a.right, r.right) - max(a.left, r.left))
        val iy = max(0f, min(a.bottom, r.bottom) - max(a.top, r.top))
        val area = max(1f, a.width() * a.height())
        return ix * iy / area
    }

    /** Ajoute un élément détouré et retire du modèle ce qu'il remplace (texte parasite lu dans un tampon, etc.). */
    fun withCutout(model: DocModel, c: Cutout): DocModel {
        val r = c.rect
        val chromatic = Colors.chroma(c.inkColor) > 50
        val texts = model.texts.filter { t ->
            val f = fractionInside(t.box, r)
            when (c.kind) {
                CutoutKind.STAMP, CutoutKind.SIGNATURE ->
                    if (chromatic) !(f > 0.5f && Colors.dist(t.color, c.inkColor) < 90) else f < 0.5f
                else -> f < 0.5f
            }
        }
        val replacesBackground = c.kind == CutoutKind.LOGO || c.kind == CutoutKind.PHOTO
        val fills = if (replacesBackground) model.fills.filter { fractionInside(it.rect, r) < 0.6f } else model.fills
        val rules = if (replacesBackground) {
            model.rules.filter { l ->
                val box = RectF(min(l.x1, l.x2), min(l.y1, l.y2), max(l.x1, l.x2) + 1f, max(l.y1, l.y2) + 1f)
                fractionInside(box, r) < 0.6f
            }
        } else {
            model.rules
        }
        return model.copy(texts = texts, fills = fills, rules = rules, cutouts = model.cutouts + c)
    }
}

object Fidelity {
    class Result(val score: Float, val heat: Bitmap)

    private fun gray(b: Bitmap, w: Int, h: Int): IntArray {
        val s = Bitmap.createScaledBitmap(b, w, h, true)
        val px = IntArray(w * h)
        s.getPixels(px, 0, w, 0, 0, w, h)
        if (s !== b) s.recycle()
        return IntArray(w * h) { Colors.lum(px[it]) }
    }

    private fun blur(src: IntArray, w: Int, h: Int): IntArray {
        val tmp = IntArray(w * h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0
                var n = 0
                for (k in -2..2) {
                    val xx = x + k
                    if (xx in 0 until w) {
                        s += src[y * w + xx]
                        n++
                    }
                }
                tmp[y * w + x] = s / n
            }
        }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0
                var n = 0
                for (k in -2..2) {
                    val yy = y + k
                    if (yy in 0 until h) {
                        s += tmp[yy * w + x]
                        n++
                    }
                }
                out[y * w + x] = s / n
            }
        }
        return out
    }

    /**
     * Score = 1 - (écart au scan / écart d'une page vierge). 100 % = reconstruction parfaite,
     * 0 % = aussi loin du scan qu'une page blanche.
     */
    fun compare(original: Bitmap, rendered: Bitmap, paperColor: Int): Result {
        val w = 800
        val h = max(1, (original.height * w / original.width.toFloat()).toInt())
        val a = blur(gray(original, w, h), w, h)
        val b = blur(gray(rendered, w, h), w, h)
        val paperGray = Colors.lum(paperColor)
        var d = 0L
        var d0 = 0L
        val heatPx = IntArray(w * h)
        for (i in 0 until w * h) {
            val diff = abs(a[i] - b[i])
            d += diff
            d0 += abs(a[i] - paperGray)
            val alpha = min(255, diff * 4)
            heatPx[i] = (alpha shl 24) or 0x00E5262F
        }
        val ratio = if (d0 <= 0L) 0f else d.toFloat() / d0.toFloat()
        val heat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        heat.setPixels(heatPx, 0, w, 0, 0, w, h)
        return Result((1f - ratio).coerceIn(0f, 1f), heat)
    }
}
