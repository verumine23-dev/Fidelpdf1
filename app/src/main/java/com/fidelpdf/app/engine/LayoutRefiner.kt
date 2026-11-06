package com.fidelpdf.app.engine

import android.content.res.AssetManager
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Finition de la mise en page : comme un maquettiste qui aligne tout à la règle.
 *  - les mots d'une même ligne partagent la même taille et la même ligne de base ;
 *  - les tailles quasi identiques du document sont unifiées (corps de texte, titres) ;
 *  - les débuts de ligne alignés sur une même colonne sont mis exactement à la même abscisse ;
 *  - les traits proches sont alignés sur une même coordonnée et se rejoignent aux jonctions.
 */
object LayoutRefiner {

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
    }

    /**
     * Grille invisible du document :
     *  1. chaque mot reçoit une taille et une ligne de base ; les lignes dont les bases sont proches
     *     (cellules d'un même rang de tableau, texte coupé par l'OCR) sont ramenées sur le même rang ;
     *  2. les bords gauches proches (colonnes) sont alignés sur une même abscisse ;
     *  3. les bords droits proches (colonnes de nombres alignées à droite) aussi.
     */
    fun refineTexts(texts: List<TextItem>, assets: AssetManager): List<TextItem> {
        if (texts.isEmpty()) return texts
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val sizes = FloatArray(texts.size)
        val bases = FloatArray(texts.size)
        for (i in texts.indices) {
            val t = texts[i]
            val p = TextFit.place(t.text, t.box, Fonts.get(assets, t.font), paint)
            sizes[i] = p.size
            bases[i] = p.baseline
        }

        val byLine = texts.indices.groupBy { texts[it].line }
        val lineSize = HashMap<Int, Float>()
        val lineBase = HashMap<Int, Float>()
        for ((ln, idx) in byLine) {
            lineSize[ln] = median(idx.map { sizes[it] })
            lineBase[ln] = median(idx.map { bases[it] })
        }

        // 1a. unification des tailles voisines (6 %) sur tout le document
        val orderedLines = lineSize.keys.sortedBy { lineSize[it] ?: 0f }
        val snappedSize = HashMap<Int, Float>()
        var start = 0
        while (start < orderedLines.size) {
            val s0 = lineSize[orderedLines[start]] ?: 0f
            var end = start
            while (end + 1 < orderedLines.size && (lineSize[orderedLines[end + 1]] ?: 0f) <= s0 * 1.06f) end++
            val m = median((start..end).map { lineSize[orderedLines[it]] ?: 0f })
            for (k in start..end) snappedSize[orderedLines[k]] = m
            start = end + 1
        }

        // 1b. rangs : lignes de base proches => même ligne de base
        val rowBase = HashMap<Int, Float>()
        val byBase = lineBase.keys.sortedBy { lineBase[it] ?: 0f }
        var rs = 0
        while (rs < byBase.size) {
            val b0 = lineBase[byBase[rs]] ?: 0f
            val s0 = snappedSize[byBase[rs]] ?: 10f
            val tol = max(1.5f, 0.15f * s0)
            var re = rs
            while (re + 1 < byBase.size && (lineBase[byBase[re + 1]] ?: 0f) - b0 <= tol) re++
            val m = median((rs..re).map { lineBase[byBase[it]] ?: 0f })
            for (k in rs..re) rowBase[byBase[k]] = m
            rs = re + 1
        }

        // 2. colonnes à gauche
        val typical = median(snappedSize.values.toList())
        val tolX = max(2.5f, 0.12f * typical)
        val n = texts.size
        val dx = FloatArray(n)
        val moved = BooleanArray(n)
        val byLeft = (0 until n).sortedBy { texts[it].box.left }
        var cs = 0
        while (cs < n) {
            val l0 = texts[byLeft[cs]].box.left
            var ce = cs
            while (ce + 1 < n && texts[byLeft[ce + 1]].box.left - l0 <= tolX) ce++
            if (ce > cs) {
                val m = median((cs..ce).map { texts[byLeft[it]].box.left })
                for (k in cs..ce) {
                    dx[byLeft[k]] = m - texts[byLeft[k]].box.left
                    moved[byLeft[k]] = true
                }
            }
            cs = ce + 1
        }

        // 3. colonnes à droite (au moins 3 textes, hors ceux déjà calés à gauche)
        val rest = (0 until n).filter { !moved[it] }.sortedBy { texts[it].box.right }
        var qs = 0
        while (qs < rest.size) {
            val r0 = texts[rest[qs]].box.right
            var qe = qs
            while (qe + 1 < rest.size && texts[rest[qe + 1]].box.right - r0 <= tolX) qe++
            if (qe - qs >= 2) {
                val m = median((qs..qe).map { texts[rest[it]].box.right })
                for (k in qs..qe) dx[rest[k]] = m - texts[rest[k]].box.right
            }
            qs = qe + 1
        }

        return texts.mapIndexed { i, t ->
            val d = dx[i]
            val b = RectF(t.box.left + d, t.box.top, t.box.right + d, t.box.bottom)
            val size = snappedSize[t.line] ?: 0f
            t.copy(
                box = b,
                fixedSize = size,
                fixedBaseline = rowBase[t.line] ?: (lineBase[t.line] ?: 0f),
                bold = Weight.extra(assets, t.font, t.stroke, size)
            )
        }
    }

    /** Valeurs proches (à [tol] près) ramenées à la médiane de leur groupe. */
    private fun cluster(values: List<Float>, tol: Float): FloatArray {
        val out = FloatArray(values.size)
        val order = values.indices.sortedBy { values[it] }
        var s = 0
        while (s < order.size) {
            val v0 = values[order[s]]
            var e = s
            while (e + 1 < order.size && values[order[e + 1]] - v0 <= tol) e++
            val m = median((s..e).map { values[order[it]] })
            for (k in s..e) out[order[k]] = m
            s = e + 1
        }
        return out
    }

    fun refineRules(rules: List<RuleLine>): List<RuleLine> {
        if (rules.isEmpty()) return rules
        val hs = rules.filter { abs(it.x2 - it.x1) >= abs(it.y2 - it.y1) }
        val vs = rules.filter { abs(it.x2 - it.x1) < abs(it.y2 - it.y1) }

        val hy = cluster(hs.map { it.y1 }, 2f)
        var h1 = hs.mapIndexed { i, r -> r.copy(y1 = hy[i], y2 = hy[i]) }
        val vx = cluster(vs.map { it.x1 }, 2f)
        var v1 = vs.mapIndexed { i, r -> r.copy(x1 = vx[i], x2 = vx[i]) }

        h1 = mergeCollinear(h1, horizontal = true)
        v1 = mergeCollinear(v1, horizontal = false)

        val tol = 8f
        val h2 = h1.map { r ->
            var x1 = r.x1
            var x2 = r.x2
            var d1 = tol + 1f
            var d2 = tol + 1f
            for (v in v1) {
                val top = min(v.y1, v.y2) - tol
                val bottom = max(v.y1, v.y2) + tol
                if (r.y1 < top || r.y1 > bottom) continue
                val a = abs(r.x1 - v.x1)
                if (a <= tol && a < d1) {
                    d1 = a
                    x1 = v.x1
                }
                val b = abs(r.x2 - v.x1)
                if (b <= tol && b < d2) {
                    d2 = b
                    x2 = v.x1
                }
            }
            r.copy(x1 = x1, x2 = x2)
        }
        val v2 = v1.map { r ->
            var y1 = r.y1
            var y2 = r.y2
            var d1 = tol + 1f
            var d2 = tol + 1f
            for (h in h1) {
                val left = min(h.x1, h.x2) - tol
                val right = max(h.x1, h.x2) + tol
                if (r.x1 < left || r.x1 > right) continue
                val a = abs(r.y1 - h.y1)
                if (a <= tol && a < d1) {
                    d1 = a
                    y1 = h.y1
                }
                val b = abs(r.y2 - h.y1)
                if (b <= tol && b < d2) {
                    d2 = b
                    y2 = h.y1
                }
            }
            r.copy(y1 = y1, y2 = y2)
        }
        return harmonize(h2 + v2)
    }

    /** Segments alignés qui se touchent (ou presque) : un seul trait continu. */
    private fun mergeCollinear(list: List<RuleLine>, horizontal: Boolean): List<RuleLine> {
        if (list.size < 2) return list
        fun pos(r: RuleLine) = if (horizontal) r.y1 else r.x1
        fun a(r: RuleLine) = if (horizontal) min(r.x1, r.x2) else min(r.y1, r.y2)
        fun b(r: RuleLine) = if (horizontal) max(r.x1, r.x2) else max(r.y1, r.y2)
        val sorted = list.sortedWith(compareBy({ pos(it) }, { a(it) }))
        val out = ArrayList<RuleLine>()
        var cur = sorted[0]
        for (k in 1 until sorted.size) {
            val r = sorted[k]
            val same = abs(pos(r) - pos(cur)) < 0.5f && abs(r.thickness - cur.thickness) <= 1.5f
            if (same && a(r) <= b(cur) + 6f) {
                val lo = min(a(cur), a(r))
                val hi = max(b(cur), b(r))
                cur = if (horizontal) cur.copy(x1 = lo, x2 = hi) else cur.copy(y1 = lo, y2 = hi)
            } else {
                out.add(cur)
                cur = r
            }
        }
        out.add(cur)
        return out
    }

    /** Épaisseurs et couleurs quasi identiques => strictement identiques (tableau homogène). */
    private fun harmonize(list: List<RuleLine>): List<RuleLine> {
        if (list.isEmpty()) return list
        val thick = cluster(list.map { it.thickness }, 0.75f)
        val groups = ArrayList<IntArray>() // somme r, g, b, nombre
        val gid = IntArray(list.size)
        val refs = ArrayList<Int>()
        for ((i, r) in list.withIndex()) {
            var g = -1
            for ((k, ref) in refs.withIndex()) {
                if (Colors.dist(ref, r.color) < 40) {
                    g = k
                    break
                }
            }
            if (g < 0) {
                refs.add(r.color)
                groups.add(IntArray(4))
                g = refs.size - 1
            }
            gid[i] = g
            groups[g][0] += Colors.red(r.color)
            groups[g][1] += Colors.green(r.color)
            groups[g][2] += Colors.blue(r.color)
            groups[g][3]++
        }
        return list.mapIndexed { i, r ->
            val g = groups[gid[i]]
            val c = (0xFF shl 24) or ((g[0] / g[3]) shl 16) or ((g[1] / g[3]) shl 8) or (g[2] / g[3])
            r.copy(thickness = max(1f, thick[i]), color = c)
        }
    }
}
