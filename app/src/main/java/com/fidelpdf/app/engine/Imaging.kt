package com.fidelpdf.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

object Colors {
    fun red(c: Int): Int = (c shr 16) and 255
    fun green(c: Int): Int = (c shr 8) and 255
    fun blue(c: Int): Int = c and 255

    fun rgb(r: Int, g: Int, b: Int): Int =
        (255 shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun lum(c: Int): Int = (red(c) * 299 + green(c) * 587 + blue(c) * 114) / 1000

    fun chroma(c: Int): Int {
        val r = red(c)
        val g = green(c)
        val b = blue(c)
        return max(r, max(g, b)) - min(r, min(g, b))
    }

    /** Distance moyenne (0..255) entre deux couleurs. */
    fun dist(a: Int, b: Int): Int =
        (abs(red(a) - red(b)) + abs(green(a) - green(b)) + abs(blue(a) - blue(b))) / 3
}

class PixelBuf(val w: Int, val h: Int, val px: IntArray) {
    fun lum(x: Int, y: Int): Int = Colors.lum(px[y * w + x])

    companion object {
        fun from(b: Bitmap): PixelBuf {
            val w = b.width
            val h = b.height
            val a = IntArray(w * h)
            b.getPixels(a, 0, w, 0, 0, w, h)
            return PixelBuf(w, h, a)
        }
    }
}

object Otsu {
    /** Seuil de luminance : classe 0 = valeurs <= seuil. */
    fun threshold(hist: IntArray): Int {
        var total = 0
        for (v in hist) total += v
        var sum = 0L
        for (i in 0..255) sum += i.toLong() * hist[i]
        var sumB = 0L
        var wB = 0
        var best = -1.0
        var thr = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t.toLong() * hist[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val between = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (between > best) {
                best = between
                thr = t
            }
        }
        return thr
    }
}

object Paper {
    private fun key(c: Int): Int =
        (((c shr 19) and 31) shl 10) or (((c shr 11) and 31) shl 5) or ((c shr 3) and 31)

    /** Couleur dominante (le papier) d'un tableau de pixels. */
    fun estimate(px: IntArray, step: Int = 3): Int {
        val hist = IntArray(32768)
        var i = 0
        while (i < px.size) {
            hist[key(px[i])]++
            i += step
        }
        var best = 0
        for (k in hist.indices) if (hist[k] > hist[best]) best = k
        var sr = 0L
        var sg = 0L
        var sb = 0L
        var n = 0
        i = 0
        while (i < px.size) {
            val c = px[i]
            if (key(c) == best) {
                sr += Colors.red(c)
                sg += Colors.green(c)
                sb += Colors.blue(c)
                n++
            }
            i += step
        }
        if (n == 0) return Color.WHITE
        return Colors.rgb((sr / n).toInt(), (sg / n).toInt(), (sb / n).toInt())
    }

    fun estimateBitmap(b: Bitmap): Int {
        val s = 400f / max(b.width, b.height)
        val w = max(1, (b.width * s).roundToInt())
        val h = max(1, (b.height * s).roundToInt())
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== b) small.recycle()
        return estimate(px, 1)
    }

    /** Un papier clair et neutre devient blanc pur (rendu net et professionnel). */
    fun output(raw: Int): Int =
        if (Colors.lum(raw) >= 200 && Colors.chroma(raw) < 30) Color.WHITE else raw
}

object Imaging {

    fun load(context: Context, uri: Uri, maxSide: Int = 3000): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalStateException("Image illisible")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Image illisible")

        var rotation = 0
        resolver.openInputStream(uri)?.use { stream ->
            val orientation = ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        val longest = max(bmp.width, bmp.height)
        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
        if (rotation != 0 || scale < 1f) {
            val m = Matrix()
            if (scale < 1f) m.postScale(scale, scale)
            if (rotation != 0) m.postRotate(rotation.toFloat())
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (out !== bmp) bmp.recycle()
            bmp = out
        }
        if (bmp.config != Bitmap.Config.ARGB_8888) {
            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
            bmp.recycle()
            bmp = copy
        }
        return bmp
    }

    /** Angle d'inclinaison du texte en degrés (positif = lignes qui descendent vers la droite). */
    fun estimateSkew(src: Bitmap): Float {
        val targetW = 700
        val s = targetW.toFloat() / src.width
        val sw = targetW
        val sh = max(8, (src.height * s).roundToInt())
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        if (small !== src) small.recycle()

        val hist = IntArray(256)
        val lums = IntArray(sw * sh)
        for (i in lums.indices) {
            val l = Colors.lum(px[i])
            lums[i] = l
            hist[l]++
        }
        val thr = Otsu.threshold(hist)
        var low = 0
        for (i in 0..thr) low += hist[i]
        val inkIsDark = low <= lums.size - low
        if (low == 0 || low == lums.size) return 0f

        val xs = ArrayList<Int>()
        val ys = ArrayList<Int>()
        val stride = max(1, (min(low, lums.size - low)) / 60000)
        var counter = 0
        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val l = lums[y * sw + x]
                val ink = if (inkIsDark) l <= thr else l > thr
                if (ink) {
                    if (counter % stride == 0) {
                        xs.add(x)
                        ys.add(y)
                    }
                    counter++
                }
            }
        }
        if (xs.size < 200) return 0f

        val off = (sw * 0.12f).toInt() + 4
        val bins = IntArray(sh + 2 * off + 2)

        fun scoreAt(deg: Float): Double {
            java.util.Arrays.fill(bins, 0)
            val t = tan(Math.toRadians(deg.toDouble())).toFloat()
            for (i in xs.indices) {
                val yy = ys[i] - (xs[i] * t).roundToInt() + off
                if (yy >= 0 && yy < bins.size) bins[yy]++
            }
            var sum = 0.0
            for (v in bins) sum += v.toDouble() * v
            return sum
        }

        val base = scoreAt(0f)
        var bestDeg = 0f
        var best = base
        var d = -6f
        while (d <= 6f) {
            val sc = scoreAt(d)
            if (sc > best) {
                best = sc
                bestDeg = d
            }
            d += 0.5f
        }
        var fine = bestDeg - 0.5f
        val fineEnd = bestDeg + 0.5f
        while (fine <= fineEnd) {
            val sc = scoreAt(fine)
            if (sc > best) {
                best = sc
                bestDeg = fine
            }
            fine += 0.1f
        }
        // Gain trop faible : on ne touche pas à l'image.
        return if (best > base * 1.03) bestDeg else 0f
    }

    /** Image destinée à l'OCR seulement : le papier est ramené au blanc pour un meilleur contraste. */
    fun normalizeForOcr(src: Bitmap, paper: Int): Bitmap {
        if (Colors.lum(paper) < 150) return src
        val gr = 255f / max(1, Colors.red(paper))
        val gg = 255f / max(1, Colors.green(paper))
        val gb = 255f / max(1, Colors.blue(paper))
        val cm = android.graphics.ColorMatrix(
            floatArrayOf(
                gr, 0f, 0f, 0f, 0f,
                0f, gg, 0f, 0f, 0f,
                0f, 0f, gb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val p = Paint()
        p.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, p)
        return out
    }


    /**
     * Supprime ombres et éclairage inégal : le fond de chaque zone est estimé (percentile élevé par bloc),
     * lissé, puis divisé. Le papier devient uniformément blanc ; les aplats sombres restent préservés.
     */
    fun flatten(src: Bitmap, paper: Int): Bitmap {
        if (Colors.lum(paper) < 90) return src
        val tmx = max(1, max(Colors.red(paper), max(Colors.green(paper), Colors.blue(paper))))
        val tint = if (Colors.chroma(paper) >= 30) {
            floatArrayOf(
                Colors.red(paper) * 255f / tmx, Colors.green(paper) * 255f / tmx, Colors.blue(paper) * 255f / tmx
            )
        } else {
            floatArrayOf(255f, 255f, 255f)
        }
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val bs = max(32, max(w, h) / 40)
        val gx = (w + bs - 1) / bs
        val gy = (h + bs - 1) / bs
        val bg = Array(3) { FloatArray(gx * gy) }
        val hist = IntArray(768)
        val floor = floatArrayOf(
            0.6f * Colors.red(paper), 0.6f * Colors.green(paper), 0.6f * Colors.blue(paper)
        )
        for (by in 0 until gy) {
            for (bx in 0 until gx) {
                java.util.Arrays.fill(hist, 0)
                val x0 = bx * bs
                val x1 = min(w, x0 + bs)
                val y0 = by * bs
                val y1 = min(h, y0 + bs)
                var n = 0
                var y = y0
                while (y < y1) {
                    var x = x0
                    while (x < x1) {
                        val c = px[y * w + x]
                        hist[(c shr 16) and 255]++
                        hist[256 + ((c shr 8) and 255)]++
                        hist[512 + (c and 255)]++
                        n++
                        x += 2
                    }
                    y += 2
                }
                val target = (n * 0.85f).toInt()
                for (ch in 0..2) {
                    var acc = 0
                    var v = 255
                    for (i in 0..255) {
                        acc += hist[ch * 256 + i]
                        if (acc >= target) {
                            v = i
                            break
                        }
                    }
                    bg[ch][by * gx + bx] = max(floor[ch], v.toFloat())
                }
            }
        }
        // lissage 3 x 3 de la grille de fond
        val sm = Array(3) { FloatArray(gx * gy) }
        for (ch in 0..2) {
            for (by in 0 until gy) {
                for (bx in 0 until gx) {
                    var sum = 0f
                    var cnt = 0
                    for (dy in -1..1) for (dx in -1..1) {
                        val xx = bx + dx
                        val yy = by + dy
                        if (xx in 0 until gx && yy in 0 until gy) {
                            sum += bg[ch][yy * gx + xx]
                            cnt++
                        }
                    }
                    sm[ch][by * gx + bx] = sum / cnt
                }
            }
        }
        val xi0 = IntArray(w)
        val xi1 = IntArray(w)
        val xw = FloatArray(w)
        for (x in 0 until w) {
            val f = (x + 0.5f) / bs - 0.5f
            val i0 = kotlin.math.floor(f).toInt()
            xi0[x] = i0.coerceIn(0, gx - 1)
            xi1[x] = (i0 + 1).coerceIn(0, gx - 1)
            xw[x] = (f - i0).coerceIn(0f, 1f)
        }
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val f = (y + 0.5f) / bs - 0.5f
            val j0 = kotlin.math.floor(f).toInt()
            val r0 = j0.coerceIn(0, gy - 1) * gx
            val r1 = (j0 + 1).coerceIn(0, gy - 1) * gx
            val yw = (f - j0).coerceIn(0f, 1f)
            for (x in 0 until w) {
                val a = xi0[x]
                val b = xi1[x]
                val wx = xw[x]
                val c = px[y * w + x]
                var res = 0xFF shl 24
                for (ch in 0..2) {
                    val g = sm[ch]
                    val top = g[r0 + a] * (1f - wx) + g[r0 + b] * wx
                    val bot = g[r1 + a] * (1f - wx) + g[r1 + b] * wx
                    val bgv = top * (1f - yw) + bot * yw
                    val v = (c shr (16 - 8 * ch)) and 255
                    val o = min(255f, v * tint[ch] / max(1f, bgv)).toInt()
                    res = res or (o shl (16 - 8 * ch))
                }
                out[y * w + x] = res
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    /**
     * Cherche la feuille dans la photo : plus grande zone claire, puis ses quatre coins extrêmes.
     * Retourne [hautG x,y, hautD x,y, basD x,y, basG x,y] en fractions de l'image, ou null si incertain.
     */
    fun detectQuad(src: Bitmap): FloatArray? {
        val sc = 320f / max(src.width, src.height)
        val w = max(8, (src.width * sc).roundToInt())
        val h = max(8, (src.height * sc).roundToInt())
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()
        val g = IntArray(w * h) { Colors.lum(px[it]) }
        // flou 5 x 5 (deux passes de boîte)
        val t1 = IntArray(w * h)
        val bl = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            var n = 0
            for (k in -2..2) {
                val xx = x + k
                if (xx in 0 until w) { s += g[y * w + xx]; n++ }
            }
            t1[y * w + x] = s / n
        }
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            var n = 0
            for (k in -2..2) {
                val yy = y + k
                if (yy in 0 until h) { s += t1[yy * w + x]; n++ }
            }
            bl[y * w + x] = s / n
        }
        val hist = IntArray(256)
        for (v in bl) hist[v.coerceIn(0, 255)]++
        val th = Otsu.threshold(hist)
        val mask = BooleanArray(w * h) { bl[it] > th }
        // plus grande composante connexe claire
        val label = IntArray(w * h)
        val queue = IntArray(w * h)
        var bestLabel = 0
        var bestCount = 0
        var cur = 0
        for (st in 0 until w * h) {
            if (!mask[st] || label[st] != 0) continue
            cur++
            var head = 0
            var tail = 0
            queue[tail++] = st
            label[st] = cur
            while (head < tail) {
                val c = queue[head++]
                val cx = c % w
                val cy = c / w
                for (d in 0 until 4) {
                    val nx = cx + (if (d == 0) 1 else if (d == 1) -1 else 0)
                    val ny = cy + (if (d == 2) 1 else if (d == 3) -1 else 0)
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val ni = ny * w + nx
                    if (!mask[ni] || label[ni] != 0) continue
                    label[ni] = cur
                    queue[tail++] = ni
                }
            }
            if (tail > bestCount) {
                bestCount = tail
                bestLabel = cur
            }
        }
        if (bestCount < w * h * 0.2f) return null
        var tl = -1e9f; var tr = -1e9f; var br = -1e9f; var bll = -1e9f
        var tlx = 0; var tly = 0; var trx = 0; var try0 = 0; var brx = 0; var bry = 0; var blx = 0; var bly = 0
        for (y in 0 until h) for (x in 0 until w) {
            if (label[y * w + x] != bestLabel) continue
            val a = -(x + y).toFloat()
            val b = (x - y).toFloat()
            val c = (x + y).toFloat()
            val d = (y - x).toFloat()
            if (a > tl) { tl = a; tlx = x; tly = y }
            if (b > tr) { tr = b; trx = x; try0 = y }
            if (c > br) { br = c; brx = x; bry = y }
            if (d > bll) { bll = d; blx = x; bly = y }
        }
        val area = 0.5f * abs(
            (tlx * try0 - trx * tly) + (trx * bry - brx * try0) + (brx * bly - blx * bry) + (blx * tly - tlx * bly)
        ).toFloat()
        if (area < w * h * 0.2f || area > bestCount * 1.6f) return null
        val fw = w.toFloat()
        val fh = h.toFloat()
        return floatArrayOf(
            (tlx + 0.5f) / fw, (tly + 0.5f) / fh, (trx + 0.5f) / fw, (try0 + 0.5f) / fh,
            (brx + 0.5f) / fw, (bry + 0.5f) / fh, (blx + 0.5f) / fw, (bly + 0.5f) / fh
        )
    }

    /** Redresse la feuille : le quadrilatère (en pixels) devient un rectangle (correction de perspective). */
    fun warpQuad(src: Bitmap, q: FloatArray): Bitmap {
        fun dist(ax: Float, ay: Float, bx: Float, by: Float) = kotlin.math.hypot(ax - bx, ay - by)
        val wTop = dist(q[0], q[1], q[2], q[3])
        val wBot = dist(q[6], q[7], q[4], q[5])
        val hLeft = dist(q[0], q[1], q[6], q[7])
        val hRight = dist(q[2], q[3], q[4], q[5])
        var ow = max(wTop, wBot)
        var oh = max(hLeft, hRight)
        val big = max(ow, oh)
        if (big > 3000f) {
            ow *= 3000f / big
            oh *= 3000f / big
        }
        val w = max(16, ow.roundToInt())
        val h = max(16, oh.roundToInt())
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        val m = Matrix()
        val dst = floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat())
        m.setPolyToPoly(q, 0, dst, 0, 4)
        c.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun rotate(src: Bitmap, degrees: Float, fill: Int): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(fill)
        val m = Matrix()
        m.postRotate(degrees, src.width / 2f, src.height / 2f)
        c.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
}
