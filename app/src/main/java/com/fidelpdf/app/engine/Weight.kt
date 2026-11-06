package com.fidelpdf.app.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * Degré de gras : on mesure l'épaisseur moyenne des traits des lettres dans le scan
 * (2 x surface d'encre / périmètre), on la compare à celle de la police choisie à la même taille,
 * et l'écart est ajouté (ou non) comme épaisseur de contour. Le PDF n'est ni plus pâle ni plus lourd que l'original.
 */
object Weight {
    private val natural = HashMap<FontChoice, Float>()

    /** Épaisseur moyenne de trait (pixels) d'un masque d'encre binaire. */
    fun strokeWidth(mask: BooleanArray, mw: Int, mh: Int, x0: Int, y0: Int, x1: Int, y1: Int): Float {
        var area = 0
        var perim = 0
        for (y in max(0, y0) until min(mh, y1)) {
            for (x in max(0, x0) until min(mw, x1)) {
                if (!mask[y * mw + x]) continue
                area++
                val edge = x == 0 || y == 0 || x == mw - 1 || y == mh - 1 ||
                    !mask[y * mw + x - 1] || !mask[y * mw + x + 1] ||
                    !mask[(y - 1) * mw + x] || !mask[(y + 1) * mw + x]
                if (edge) perim++
            }
        }
        if (perim < 6) return 0f
        return 2f * area / perim
    }

    /** Mesure d'un mot à partir de son encre. */
    fun measure(ink: Ink): Float {
        val m = ink.mask ?: return 0f
        val bx0 = ink.box.left.toInt() - ink.ox
        val by0 = ink.box.top.toInt() - ink.oy
        val bx1 = ink.box.right.toInt() - ink.ox
        val by1 = ink.box.bottom.toInt() - ink.oy
        return strokeWidth(m, ink.cw, ink.ch, bx0, by0, bx1, by1)
    }

    /** Épaisseur naturelle (en fraction de la taille) d'une police : mesurée une fois sur un échantillon rendu. */
    fun naturalRatio(assets: AssetManager, f: FontChoice): Float {
        synchronized(natural) {
            natural[f]?.let { return it }
            val size = 120f
            val bmp = Bitmap.createBitmap(1100, 190, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Fonts.get(assets, f)
                textSize = size
                color = Color.BLACK
            }
            c.drawText("Hlnoerst1048", 10f, 140f, p)
            val px = IntArray(1100 * 190)
            bmp.getPixels(px, 0, 1100, 0, 0, 1100, 190)
            bmp.recycle()
            val mask = BooleanArray(px.size) { (px[it] and 0xFF) < 128 }
            val w = strokeWidth(mask, 1100, 190, 0, 0, 1100, 190)
            val r = if (w <= 0f) 0.09f else w / size
            natural[f] = r
            return r
        }
    }

    /** Épaisseur de contour à ajouter pour atteindre le gras mesuré (0 = rien à ajouter). */
    fun extra(assets: AssetManager, f: FontChoice, measured: Float, size: Float): Float {
        if (measured <= 0f || size <= 0f) return 0f
        val base = naturalRatio(assets, f) * size
        val diff = measured - base * 1.04f
        if (diff < 0.035f * size) return 0f
        return min(diff, 0.1f * size)
    }
}
