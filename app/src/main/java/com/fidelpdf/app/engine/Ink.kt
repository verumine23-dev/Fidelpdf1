package com.fidelpdf.app.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Mesure de l'encre d'un mot dans le scan : boîte exacte (au pixel), couleur du texte,
 * et masque binaire utilisé ensuite pour reconnaître la police.
 */
class Ink(
    val box: RectF,
    val color: Int,
    val mask: BooleanArray?,
    val ox: Int,
    val oy: Int,
    val cw: Int,
    val ch: Int
) {

    /** Densité d'encre (0..1) de la boîte, échantillonnée sur une grille tw x th. */
    fun density(tw: Int, th: Int): FloatArray {
        val m = mask ?: return FloatArray(tw * th)
        val sum = FloatArray(tw * th)
        val cnt = IntArray(tw * th)
        val bx0 = box.left.toInt()
        val by0 = box.top.toInt()
        val bx1 = box.right.toInt()
        val by1 = box.bottom.toInt()
        val bw = max(1, bx1 - bx0)
        val bh = max(1, by1 - by0)
        for (y in by0 until by1) {
            val my = y - oy
            if (my < 0 || my >= ch) continue
            val ty = min(th - 1, (y - by0) * th / bh)
            for (x in bx0 until bx1) {
                val mx = x - ox
                if (mx < 0 || mx >= cw) continue
                val tx = min(tw - 1, (x - bx0) * tw / bw)
                val i = ty * tw + tx
                cnt[i]++
                if (m[my * cw + mx]) sum[i] += 1f
            }
        }
        return FloatArray(tw * th) { if (cnt[it] == 0) 0f else sum[it] / cnt[it] }
    }

    companion object {
        fun fallback(r: Rect): Ink = Ink(RectF(r), Color.BLACK, null, 0, 0, 0, 0)

        fun measure(pix: PixelBuf, r: Rect): Ink? {
            val pad = max(2, (r.height() * 0.12f).toInt())
            val x0 = max(0, r.left - pad)
            val y0 = max(0, r.top - pad)
            val x1 = min(pix.w, r.right + pad)
            val y1 = min(pix.h, r.bottom + pad)
            val cw = x1 - x0
            val ch = y1 - y0
            if (cw < 3 || ch < 3) return null

            val lumArr = IntArray(cw * ch)
            val hist = IntArray(256)
            for (y in 0 until ch) {
                val base = (y0 + y) * pix.w + x0
                for (x in 0 until cw) {
                    val l = Colors.lum(pix.px[base + x])
                    lumArr[y * cw + x] = l
                    hist[l]++
                }
            }
            val thr = Otsu.threshold(hist)
            var low = 0
            var lowSum = 0L
            var highSum = 0L
            for (i in 0..255) {
                if (i <= thr) {
                    low += hist[i]
                    lowSum += i.toLong() * hist[i]
                } else {
                    highSum += i.toLong() * hist[i]
                }
            }
            val high = cw * ch - low
            if (low == 0 || high == 0) return null
            val meanLow = lowSum.toFloat() / low
            val meanHigh = highSum.toFloat() / high
            if (meanHigh - meanLow < 40f) return null

            val inkIsDark = low <= high
            val mask = BooleanArray(cw * ch) { if (inkIsDark) lumArr[it] <= thr else lumArr[it] > thr }

            // Les traits de tableau (lignes presque pleines) ne font pas partie du texte.
            val rowCnt = IntArray(ch)
            val colCnt = IntArray(cw)
            for (y in 0 until ch) for (x in 0 until cw) {
                if (mask[y * cw + x]) {
                    rowCnt[y]++
                    colCnt[x]++
                }
            }
            for (y in 0 until ch) {
                if (rowCnt[y] >= 0.8f * cw) for (x in 0 until cw) mask[y * cw + x] = false
            }
            for (x in 0 until cw) {
                if (colCnt[x] >= 0.8f * ch) for (y in 0 until ch) mask[y * cw + x] = false
            }
            java.util.Arrays.fill(rowCnt, 0)
            java.util.Arrays.fill(colCnt, 0)
            for (y in 0 until ch) for (x in 0 until cw) {
                if (mask[y * cw + x]) {
                    rowCnt[y]++
                    colCnt[x]++
                }
            }

            val minR = if (cw > 150) 2 else 1
            val minC = if (ch > 40) 2 else 1
            var top = -1
            var bottom = -1
            var left = -1
            var right = -1
            for (y in 0 until ch) if (rowCnt[y] >= minR) {
                if (top < 0) top = y
                bottom = y
            }
            for (x in 0 until cw) if (colCnt[x] >= minC) {
                if (left < 0) left = x
                right = x
            }
            if (top < 0 || left < 0) return null

            // Couleur de fond locale (pixels de l'autre classe)
            var br = 0L
            var bg = 0L
            var bb = 0L
            var bn = 0
            for (i in 0 until cw * ch) {
                val isInkClass = if (inkIsDark) lumArr[i] <= thr else lumArr[i] > thr
                if (!isInkClass) {
                    val c = pix.px[(y0 + i / cw) * pix.w + x0 + i % cw]
                    br += Colors.red(c)
                    bg += Colors.green(c)
                    bb += Colors.blue(c)
                    bn++
                }
            }
            val bgColor = if (bn > 0) Colors.rgb((br / bn).toInt(), (bg / bn).toInt(), (bb / bn).toInt()) else Color.WHITE

            // Couleur du texte : moyenne des pixels d'encre les plus éloignés du fond
            val dh = IntArray(256)
            var inkCount = 0
            for (y in top..bottom) for (x in left..right) {
                if (mask[y * cw + x]) {
                    val c = pix.px[(y0 + y) * pix.w + x0 + x]
                    dh[min(255, Colors.dist(c, bgColor))]++
                    inkCount++
                }
            }
            var color = if (inkIsDark) Color.BLACK else Color.WHITE
            if (inkCount > 0) {
                val topN = max(1, inkCount / 10)
                var acc = 0
                var peak = 0
                for (v in 255 downTo 0) {
                    acc += dh[v]
                    if (acc >= topN) {
                        peak = v
                        break
                    }
                }
                val cutoff = (peak * 0.8f).toInt()
                var sr = 0L
                var sg = 0L
                var sb = 0L
                var n = 0
                for (y in top..bottom) for (x in left..right) {
                    if (mask[y * cw + x]) {
                        val c = pix.px[(y0 + y) * pix.w + x0 + x]
                        if (Colors.dist(c, bgColor) >= cutoff) {
                            sr += Colors.red(c)
                            sg += Colors.green(c)
                            sb += Colors.blue(c)
                            n++
                        }
                    }
                }
                if (n > 0) {
                    color = Colors.rgb((sr / n).toInt(), (sg / n).toInt(), (sb / n).toInt())
                    val mx = max(Colors.red(color), max(Colors.green(color), Colors.blue(color)))
                    val mn = min(Colors.red(color), min(Colors.green(color), Colors.blue(color)))
                    // Noir ou blanc « presque neutres » : on les ramène à du noir / blanc pur.
                    if (mx < 100 && mx - mn < 40) color = Color.BLACK
                    if (mn > 190 && mx - mn < 40) color = Color.WHITE
                }
            }

            val box = RectF(
                (x0 + left).toFloat(),
                (y0 + top).toFloat(),
                (x0 + right + 1).toFloat(),
                (y0 + bottom + 1).toFloat()
            )
            return Ink(box, color, mask, x0, y0, cw, ch)
        }
    }
}

/** Reconnaissance de la police par « rendu et comparaison » sur les polices embarquées. */
class FontScorer(private val assets: AssetManager) {
    val candidates: List<FontChoice> = FontFamilyId.values().flatMap { f ->
        listOf(false, true).flatMap { b -> listOf(false, true).map { i -> FontChoice(f, b, i) } }
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Score de chaque candidat (plus petit = meilleur). */
    fun scores(text: String, ink: Ink): FloatArray {
        val out = FloatArray(candidates.size)
        if (ink.mask == null || text.isBlank()) return out
        val th = 24
        val bw = ink.box.width()
        val bh = max(1f, ink.box.height())
        val tw = (th * bw / bh).roundToInt().coerceIn(8, 360)
        val target = ink.density(tw, th)
        val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pixels = IntArray(tw * th)
        val fitBox = RectF(0f, 0f, tw.toFloat(), th.toFloat())
        for ((i, c) in candidates.withIndex()) {
            bmp.eraseColor(0)
            val tf = Fonts.get(assets, c)
            val p = TextFit.place(text, fitBox, tf, paint)
            paint.typeface = tf
            paint.textSize = p.size
            paint.textScaleX = p.scaleX
            paint.color = Color.BLACK
            canvas.drawText(text, p.x, p.baseline, paint)
            bmp.getPixels(pixels, 0, tw, 0, 0, tw, th)
            var s = 0f
            for (k in pixels.indices) {
                val a = (pixels[k] ushr 24) / 255f
                s += abs(a - target[k])
            }
            s /= pixels.size
            // Une police dont la largeur naturelle diverge beaucoup est probablement la mauvaise.
            out[i] = s + 0.3f * abs(ln(p.scaleX))
        }
        bmp.recycle()
        return out
    }
}
