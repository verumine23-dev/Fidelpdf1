package com.fidelpdf.app.engine

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Reconstruction géométrique des tableaux et traits.
 *
 * Un papier plié, froissé ou photographié de biais donne des traits décalés, brisés ou épaissis.
 * Au lieu de les recopier, on s'appuie sur ce que l'on sait d'un tableau :
 *  - un trait est une droite horizontale ou verticale qui va d'un point A à un point B ;
 *  - des fragments proches et alignés appartiennent au même trait (position = médiane, pas moyenne) ;
 *  - les extrémités se terminent sur le trait perpendiculaire : angles droits, cellules fermées ;
 *  - un trait isolé et pâle (ombre d'un pli) n'est pas un trait ;
 *  - tous les traits d'une même sorte ont exactement la même épaisseur et la même couleur.
 */
object TableGrid {

    private class Seg(var pos: Float, var a: Float, var b: Float, var thick: Float, var color: Int) {
        val length: Float get() = b - a
    }

    fun build(rules: List<RuleLine>, w: Int, h: Int): List<RuleLine> {
        if (rules.isEmpty()) return rules
        val big = max(w, h).toFloat()

        val hs = ArrayList<Seg>()
        val vs = ArrayList<Seg>()
        for (r in rules) {
            if (abs(r.x2 - r.x1) >= abs(r.y2 - r.y1)) {
                hs.add(Seg((r.y1 + r.y2) / 2f, min(r.x1, r.x2), max(r.x1, r.x2), r.thickness, r.color))
            } else {
                vs.add(Seg((r.x1 + r.x2) / 2f, min(r.y1, r.y2), max(r.y1, r.y2), r.thickness, r.color))
            }
        }

        val tolPos = max(6f, 0.0035f * big)
        val gapMerge = max(14f, 0.02f * big)
        var H = consolidate(hs, tolPos, gapMerge)
        var V = consolidate(vs, tolPos, gapMerge)

        // Cadre extérieur prioritaire : le trait le plus extérieur de chaque côté remplace ses doublons décalés,
        // les traînées hors cadre disparaissent, les traits intérieurs sont recadrés
        val frame = applyFrame(H, V, big)
        H = frame.first
        V = frame.second

        // Fermeture : chaque extrémité se pose sur le trait perpendiculaire le plus proche (angles droits)
        val snap = max(12f, 0.012f * big)
        for (pass in 0 until 2) {
            snapEnds(H, V, snap)
            snapEnds(V, H, snap)
        }

        // Traits isolés et pâles (ombres de pli) ou minuscules : supprimés
        val minLen = 0.03f * big
        val whiteDist = 100
        H = H.filter { keep(it, V, snap, minLen, whiteDist) }.toMutableList()
        V = V.filter { keep(it, H, snap, minLen, whiteDist) }.toMutableList()

        val all = ArrayList<Seg>()
        all.addAll(H)
        all.addAll(V)
        harmonize(all)

        val out = ArrayList<RuleLine>()
        for (s in H) out.add(RuleLine(s.a, s.pos, s.b, s.pos, s.thick, s.color))
        for (s in V) out.add(RuleLine(s.pos, s.a, s.pos, s.b, s.thick, s.color))
        return out
    }

    /**
     * Détecte le cadre du tableau : de chaque côté, parmi les traits longs, le plus extérieur gagne.
     * Les traits parallèles proches (doublons décalés par un pli) sont absorbés par lui, le cadre est étendu
     * sur toute la largeur/hauteur, puis tout ce qui dépasse est coupé et les petites traînées extérieures supprimées.
     */
    private fun applyFrame(H: MutableList<Seg>, V: MutableList<Seg>, big: Float): Pair<MutableList<Seg>, MutableList<Seg>> {
        val maxH = H.maxOfOrNull { it.length } ?: 0f
        val maxV = V.maxOfOrNull { it.length } ?: 0f
        if (maxH < 0.15f * big || maxV < 0.15f * big) return Pair(H, V)
        val longH = H.filter { it.length >= 0.6f * maxH }
        val longV = V.filter { it.length >= 0.6f * maxV }
        val top = longH.minByOrNull { it.pos }!!
        val bottom = longH.maxByOrNull { it.pos }!!
        val left = longV.minByOrNull { it.pos }!!
        val right = longV.maxByOrNull { it.pos }!!
        val hasBottom = bottom.pos - top.pos > 0.1f * big
        val hasRight = right.pos - left.pos > 0.1f * big

        val fh = if (hasBottom) bottom.pos - top.pos else big
        val fw = if (hasRight) right.pos - left.pos else big
        val bandY = 0.022f * fh
        val bandX = 0.022f * fw

        val topPos = top.pos
        val botPos = if (hasBottom) bottom.pos else top.pos
        val leftPos = left.pos
        val rightPos = if (hasRight) right.pos else left.pos
        val xFrom = min(top.a, if (hasBottom) bottom.a else top.a)
        val xTo = max(top.b, if (hasBottom) bottom.b else top.b)
        val yFrom = min(left.a, if (hasRight) right.a else left.a)
        val yTo = max(left.b, if (hasRight) right.b else left.b)
        val fx0 = min(xFrom, leftPos)
        val fx1 = max(xTo, rightPos)
        val fy0 = min(yFrom, topPos)
        val fy1 = max(yTo, botPos)

        val newH = ArrayList<Seg>()
        val newV = ArrayList<Seg>()
        val shortLimit = 0.12f * big

        // cadre : 2 à 4 traits droits identiques de A à B
        fun mk(pos: Float, a: Float, b: Float, src: Seg) = Seg(pos, a, b, src.thick, src.color)
        newH.add(mk(topPos, fx0, fx1, top))
        if (hasBottom) newH.add(mk(botPos, fx0, fx1, bottom))
        newV.add(mk(leftPos, fy0, fy1, left))
        if (hasRight) newV.add(mk(rightPos, fy0, fy1, right))

        for (s in H) {
            if (s === top || s === bottom) continue
            if (abs(s.pos - topPos) <= bandY || (hasBottom && abs(s.pos - botPos) <= bandY)) continue // doublon d'un côté
            if (s.pos < topPos - bandY || (hasBottom && s.pos > botPos + bandY)) {
                if (s.length < shortLimit) continue // traînée hors cadre
                newH.add(s)
                continue
            }
            if (s.b < fx0 || s.a > fx1) continue
            s.a = max(s.a, fx0)
            s.b = min(s.b, fx1)
            if (s.length > 2f) newH.add(s)
        }
        for (s in V) {
            if (s === left || s === right) continue
            if (abs(s.pos - leftPos) <= bandX || (hasRight && abs(s.pos - rightPos) <= bandX)) continue
            if (s.pos < leftPos - bandX || (hasRight && s.pos > rightPos + bandX)) {
                if (s.length < shortLimit) continue
                newV.add(s)
                continue
            }
            if (s.b < fy0 || s.a > fy1) continue
            s.a = max(s.a, fy0)
            s.b = min(s.b, fy1)
            if (s.length > 2f) newV.add(s)
        }
        return Pair(newH, newV)
    }

    /**
     * Après redressement des traits, replace dans sa cellule tout texte qui chevauche un trait :
     * le contenu suit la position privilégiée des traits au lieu de rester sur l'ancien tracé.
     */
    fun justify(rules: List<RuleLine>, texts: List<TextItem>): List<TextItem> {
        if (rules.isEmpty() || texts.isEmpty()) return texts
        val hs = rules.filter { abs(it.x2 - it.x1) >= abs(it.y2 - it.y1) }
        val vs = rules.filter { abs(it.x2 - it.x1) < abs(it.y2 - it.y1) }
        if (hs.size < 2 || vs.size < 2) return texts
        val minX = vs.minOf { it.x1 }
        val maxX = vs.maxOf { it.x1 }
        val minY = hs.minOf { it.y1 }
        val maxY = hs.maxOf { it.y1 }
        return texts.map { t ->
            val b = t.box
            val cx = b.centerX()
            val cy = b.centerY()
            if (cx < minX || cx > maxX || cy < minY || cy > maxY) return@map t
            val th = max(1f, b.height())
            val slack = max(th * 1.2f, 20f)
            val pad = max(3f, th * 0.18f)
            var top = minY
            var bottom = maxY
            for (r in hs) {
                val xa = min(r.x1, r.x2)
                val xb = max(r.x1, r.x2)
                if (cx < xa || cx > xb) continue
                if (r.y1 <= cy && r.y1 > top) top = r.y1
                if (r.y1 >= cy && r.y1 < bottom) bottom = r.y1
            }
            var left = minX
            var right = maxX
            for (r in vs) {
                val ya = min(r.y1, r.y2)
                val yb = max(r.y1, r.y2)
                if (cy < ya || cy > yb) continue
                if (r.x1 <= cx && r.x1 > left) left = r.x1
                if (r.x1 >= cx && r.x1 < right) right = r.x1
            }
            var dx = 0f
            var dy = 0f
            if (b.left < left + pad) dx = left + pad - b.left
            else if (b.right > right - pad && b.width() <= right - left - 2 * pad) dx = right - pad - b.right
            if (b.top < top + pad) dy = top + pad - b.top
            else if (b.bottom > bottom - pad) dy = bottom - pad - b.bottom
            if (b.width() > right - left - 2 * pad) dx = if (dx > 0f) dx else 0f
            if (abs(dx) > slack) dx = 0f
            if (abs(dy) > slack) dy = 0f
            if (dx == 0f && dy == 0f) t
            else t.copy(
                box = RectF(b.left + dx, b.top + dy, b.right + dx, b.bottom + dy),
                fixedBaseline = if (t.fixedBaseline != 0f) t.fixedBaseline + dy else 0f
            )
        }
    }

    private fun wMedian(values: List<Pair<Float, Float>>): Float {
        val sorted = values.sortedBy { it.first }
        val total = sorted.sumOf { it.second.toDouble() }
        var acc = 0.0
        for (p in sorted) {
            acc += p.second
            if (acc >= total / 2) return p.first
        }
        return sorted.last().first
    }

    /** Regroupe les fragments alignés (même position à tol près), puis recolle ceux qui se suivent. */
    private fun consolidate(list: List<Seg>, tol: Float, gap: Float): MutableList<Seg> {
        val out = ArrayList<Seg>()
        if (list.isEmpty()) return out
        val seeds = list.sortedByDescending { it.length }
        val clusters = ArrayList<MutableList<Seg>>()
        val centers = ArrayList<Float>()
        for (s in seeds) {
            var best = -1
            var bestD = tol + 1f
            for (k in centers.indices) {
                val d = abs(centers[k] - s.pos)
                if (d <= tol && d < bestD) {
                    bestD = d
                    best = k
                }
            }
            if (best >= 0) {
                clusters[best].add(s)
            } else {
                clusters.add(arrayListOf(s))
                centers.add(s.pos)
            }
        }
        for (c in clusters) {
            val pos = wMedian(c.map { it.pos to max(1f, it.length) })
            val sorted = c.sortedBy { it.a }
            var run = ArrayList<Seg>()
            var runEnd = -1e9f
            fun flush() {
                if (run.isEmpty()) return
                val a = run.minOf { it.a }
                val b = run.maxOf { it.b }
                val thick = wMedian(run.map { it.thick to max(1f, it.length) })
                // couleur du plus long fragment : le plus fidèle au trait réel
                val color = run.maxByOrNull { it.length }!!.color
                out.add(Seg(pos, a, b, max(1f, thick), color))
                run = ArrayList()
            }
            for (s in sorted) {
                if (run.isNotEmpty() && s.a > runEnd + gap) flush()
                run.add(s)
                runEnd = if (run.size == 1) s.b else max(runEnd, s.b)
            }
            flush()
        }
        return out
    }

    /** Aligne les extrémités de [lines] sur les traits perpendiculaires [cross] (à [snap] près). */
    private fun snapEnds(lines: List<Seg>, cross: List<Seg>, snap: Float) {
        for (s in lines) {
            var bestA = snap + 1f
            var bestB = snap + 1f
            var newA = s.a
            var newB = s.b
            for (c in cross) {
                // le trait perpendiculaire doit couvrir la position de s (avec marge)
                if (s.pos < c.a - snap || s.pos > c.b + snap) continue
                val da = abs(c.pos - s.a)
                if (da <= snap && da < bestA) {
                    bestA = da
                    newA = c.pos
                }
                val db = abs(c.pos - s.b)
                if (db <= snap && db < bestB) {
                    bestB = db
                    newB = c.pos
                }
            }
            if (newB - newA > 2f) {
                s.a = newA
                s.b = newB
            }
        }
    }

    private fun touches(s: Seg, cross: List<Seg>, snap: Float): Int {
        var n = 0
        for (c in cross) {
            if (s.pos >= c.a - snap && s.pos <= c.b + snap && c.pos >= s.a - snap && c.pos <= s.b + snap) n++
        }
        return n
    }

    private fun keep(s: Seg, cross: List<Seg>, snap: Float, minLen: Float, whiteDist: Int): Boolean {
        val connected = touches(s, cross, snap) >= 1
        if (connected) return true
        if (s.length < minLen) return false
        val faint = Colors.dist(s.color, android.graphics.Color.WHITE) < whiteDist - 15
        return !faint
    }

    /** Même épaisseur et même couleur pour les traits semblables ; épaisseur excessive plafonnée. */
    private fun harmonize(all: List<Seg>) {
        if (all.isEmpty()) return
        val med = all.map { it.thick }.sorted()[all.size / 2]
        val cap = max(2f, 3.5f * med)
        for (s in all) s.thick = min(s.thick, cap)

        // classes d'épaisseur (écart relatif de 35 %)
        val bySize = all.sortedBy { it.thick }
        var i = 0
        while (i < bySize.size) {
            val t0 = bySize[i].thick
            var j = i
            while (j + 1 < bySize.size && bySize[j + 1].thick <= t0 * 1.35f + 0.5f) j++
            val m = bySize.subList(i, j + 1).map { it.thick }.sorted().let { it[it.size / 2] }
            for (k in i..j) bySize[k].thick = m
            i = j + 1
        }

        // couleurs : groupes à moins de 40, moyenne du groupe
        val refs = ArrayList<Int>()
        val sums = ArrayList<IntArray>()
        val gid = IntArray(all.size)
        for ((k, s) in all.withIndex()) {
            var g = -1
            for ((q, ref) in refs.withIndex()) {
                if (Colors.dist(ref, s.color) < 40) {
                    g = q
                    break
                }
            }
            if (g < 0) {
                refs.add(s.color)
                sums.add(IntArray(4))
                g = refs.size - 1
            }
            gid[k] = g
            sums[g][0] += Colors.red(s.color)
            sums[g][1] += Colors.green(s.color)
            sums[g][2] += Colors.blue(s.color)
            sums[g][3]++
        }
        for ((k, s) in all.withIndex()) {
            val g = sums[gid[k]]
            s.color = (0xFF shl 24) or ((g[0] / g[3]) shl 16) or ((g[1] / g[3]) shl 8) or (g[2] / g[3])
        }
    }
}
