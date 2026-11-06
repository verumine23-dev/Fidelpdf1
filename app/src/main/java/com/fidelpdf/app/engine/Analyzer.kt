package com.fidelpdf.app.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pipeline complet : redressement -> OCR -> traits/tableaux -> aplats de couleur ->
 * polices -> modèle de document vectoriel.
 */
class Analyzer(private val assets: AssetManager) {

    class Output(
        /** Photo redressée telle que prise (affichée à l'utilisateur). */
        val bitmap: Bitmap,
        /** Même image sans ombres ni éclairage inégal : référence des comparaisons et des détourages. */
        val work: Bitmap,
        val model: DocModel,
        val suggestions: List<RectF>,
        /** Écriture manuscrite détectée : recollée comme image plutôt que lue comme du texte. */
        val autoCutouts: List<Cutout>
    )

    private class WordBox(val text: String, val rect: Rect)
    private class Measured(val word: WordBox, val ink: Ink)

    suspend fun run(source: Bitmap, progress: (Int, String) -> Unit): Output {
        progress(0, "Nettoyage et redressement de l'image")
        val skew = Imaging.estimateSkew(source)
        val bmp = if (abs(skew) >= 0.15f) {
            Imaging.rotate(source, -skew, Paper.output(Paper.estimateBitmap(source)))
        } else {
            source
        }
        val rawPaper = Paper.estimate(PixelBuf.from(bmp).px)
        val work = Imaging.flatten(bmp, rawPaper)
        val pix = PixelBuf.from(work)
        val paperRaw = Paper.estimate(pix.px)
        val paperOut = Paper.output(paperRaw)

        progress(1, "Lecture du texte (OCR)")
        val ocr = recognize(work)
        val lines = ArrayList<List<WordBox>>()
        for (block in ocr.textBlocks) {
            for (line in block.lines) {
                val ws = ArrayList<WordBox>()
                for (e in line.elements) {
                    val r = e.boundingBox
                    if (r != null && e.text.isNotBlank()) ws.add(WordBox(TextCleanup.fixWord(e.text), r))
                }
                if (ws.isNotEmpty()) lines.add(ws)
            }
        }

        progress(2, "Détection des tableaux, traits et aplats")
        val textRects = ArrayList<Rect>()
        for (l in lines) for (w in l) textRects.add(w.rect)
        val fills = detectFills(pix, paperRaw)
        val rules = detectRules(pix, paperRaw, textRects)
        val suggestions = detectStamps(pix)

        progress(3, "Analyse des polices, tailles et couleurs")
        val measuredLines = ArrayList<List<Measured>>()
        for (ws in lines) {
            val ms = ArrayList<Measured>()
            for (w in ws) {
                val ink = Ink.measure(pix, w.rect) ?: Ink.fallback(w.rect)
                ms.add(Measured(w, ink))
            }
            measuredLines.add(ms)
        }
        val scorer = FontScorer(assets)
        val nCand = scorer.candidates.size
        val lineScores = ArrayList<FloatArray>()
        for (ms in measuredLines) {
            val acc = FloatArray(nCand)
            var wsum = 0f
            for (m in ms) {
                val s = scorer.scores(m.word.text, m.ink)
                val wgt = max(1, m.word.text.length).toFloat()
                for (k in 0 until nCand) acc[k] += s[k] * wgt
                wsum += wgt
            }
            for (k in 0 until nCand) acc[k] /= wsum
            lineScores.add(acc)
        }
        val fontsAll = chooseFonts(scorer.candidates, lineScores, measuredLines)

        // Écriture manuscrite : une ligne que aucune police ne reproduit est recollée comme image
        val autoCutouts = ArrayList<Cutout>()
        val keepIdx = ArrayList<Int>()
        val flagged = detectHandwriting(measuredLines, lineScores)
        var autoId = 10000
        for (i in measuredLines.indices) {
            if (i in flagged) {
                val ms = measuredLines[i]
                val box = RectF(ms[0].ink.box)
                for (m in ms) box.union(m.ink.box)
                val padX = max(6f, box.height() * 0.3f)
                val padY = max(6f, box.height() * 0.35f)
                val rect = RectF(box.left - padX, box.top - padY, box.right + padX, box.bottom + padY)
                val c = Cutouts.extract(pix, rect, CutoutKind.SIGNATURE, autoId)
                if (c != null) {
                    autoCutouts.add(c)
                    autoId++
                    continue
                }
            }
            keepIdx.add(i)
        }
        val keptLines = keepIdx.map { measuredLines[it] }
        val fonts = keepIdx.map { fontsAll[it] }

        progress(4, "Construction du PDF vectoriel")
        val gridRules = TableGrid.build(rules, bmp.width, bmp.height)
        val texts = TableGrid.justify(gridRules, LayoutRefiner.refineTexts(buildTexts(keptLines, fonts), assets))
        val model = DocModel(
            width = bmp.width,
            height = bmp.height,
            paperColor = paperOut,
            fills = fills,
            rules = gridRules,
            texts = texts,
            cutouts = emptyList()
        )
        return Output(bmp, work, model, suggestions, autoCutouts)
    }

    // ---------------------------------------------------------------- OCR

    private suspend fun recognize(bmp: Bitmap): Text = suspendCancellableCoroutine { cont ->
        val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        rec.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { t ->
                rec.close()
                if (cont.isActive) cont.resume(t)
            }
            .addOnFailureListener { e ->
                rec.close()
                if (cont.isActive) cont.resumeWithException(e)
            }
    }

    /**
     * Repère les lignes manuscrites : la meilleure police reste très loin de l'encre mesurée
     * (par rapport aux autres lignes du document), ou l'encre est bleue et peu ressemblante.
     */
    private fun detectHandwriting(lines: List<List<Measured>>, scores: List<FloatArray>): Set<Int> {
        val n = lines.size
        if (n < 4) return emptySet()
        val best = FloatArray(n) { i -> scores[i].min() }
        val sorted = best.sorted()
        val med = sorted[n / 2]
        val devs = best.map { abs(it - med) }.sorted()
        val mad = devs[n / 2]
        val heights = lines.map { ms ->
            val b = RectF(ms[0].ink.box)
            for (m in ms) b.union(m.ink.box)
            b.height()
        }
        val medH = heights.sorted()[n / 2]
        val out = HashSet<Int>()
        for (i in 0 until n) {
            val ms = lines[i]
            val chars = ms.sumOf { it.word.text.length }
            if (chars < 3) continue
            val c = ms.maxByOrNull { it.word.text.length }!!.ink.color
            val blue = Colors.chroma(c) > 55 && Colors.blue(c) > Colors.red(c) + 30 && Colors.blue(c) > Colors.green(c) + 5
            val outlier = best[i] > max(med * 1.9f, med + 3.5f * mad)
            val softOutlier = best[i] > med * 1.35f
            val huge = heights[i] > medH * 2.6f && best[i] > med * 1.2f
            if (outlier || (blue && softOutlier) || huge) out.add(i)
        }
        // Si presque tout le document est « inhabituel », c'est la référence qui est mauvaise : on ne touche à rien.
        return if (out.size > n * 0.6f) emptySet() else out
    }

    // ---------------------------------------------------------------- Polices

    /** Choisit une police par ligne, puis harmonise sur la famille dominante du document. */
    private fun chooseFonts(
        candidates: List<FontChoice>,
        lineScores: List<FloatArray>,
        lines: List<List<Measured>>
    ): List<FontChoice> {
        val bestIdx = IntArray(lineScores.size)
        val weight = FloatArray(lineScores.size)
        val tally = HashMap<FontFamilyId, Float>()
        for (i in lineScores.indices) {
            var b = 0
            for (k in candidates.indices) if (lineScores[i][k] < lineScores[i][b]) b = k
            bestIdx[i] = b
            var chars = 0
            for (m in lines[i]) chars += m.word.text.length
            weight[i] = max(1, chars).toFloat()
            val fam = candidates[b].family
            tally[fam] = (tally[fam] ?: 0f) + weight[i]
        }
        var major: FontFamilyId = FontFamilyId.ARIAL
        var majorW = -1f
        for ((fam, w) in tally) {
            if (w > majorW) {
                majorW = w
                major = fam
            }
        }
        val result = ArrayList<FontChoice>()
        for (i in lineScores.indices) {
            val overall = lineScores[i][bestIdx[i]]
            var bestMajor = -1
            for (k in candidates.indices) {
                if (candidates[k].family == major) {
                    if (bestMajor < 0 || lineScores[i][k] < lineScores[i][bestMajor]) bestMajor = k
                }
            }
            val pick = if (bestMajor >= 0 && lineScores[i][bestMajor] <= overall * 1.3f + 0.01f) bestMajor else bestIdx[i]
            result.add(candidates[pick])
        }
        return result
    }

    /** Une ligne = un seul texte si le calage est parfait, sinon un texte par mot (positions exactes). */
    private fun buildTexts(lines: List<List<Measured>>, fonts: List<FontChoice>): List<TextItem> {
        val out = ArrayList<TextItem>()
        val scratch = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect()
        var nextId = 0
        for ((li, ms) in lines.withIndex()) {
            val font = fonts[li]
            val tf = Fonts.get(assets, font)
            val lineBox = RectF(ms[0].ink.box)
            for (m in ms) lineBox.union(m.ink.box)
            val text = ms.joinToString(" ") { it.word.text }
            var asLine = true
            if (ms.size > 1) {
                val place = TextFit.place(text, lineBox, tf, scratch)
                if (place.scaleX < 0.8f || place.scaleX > 1.25f) {
                    asLine = false
                } else {
                    scratch.typeface = tf
                    scratch.textSize = place.size
                    scratch.textScaleX = 1f
                    var prefix = ""
                    for (m in ms) {
                        val adv = scratch.measureText(prefix)
                        scratch.getTextBounds(m.word.text, 0, m.word.text.length, rect)
                        val predicted = place.x + place.scaleX * (adv + rect.left)
                        if (abs(predicted - m.ink.box.left) > 0.35f * lineBox.height()) {
                            asLine = false
                            break
                        }
                        if (Colors.dist(m.ink.color, ms[0].ink.color) > 80) {
                            asLine = false
                            break
                        }
                        prefix += m.word.text + " "
                    }
                }
            }
            if (asLine) {
                var longest = ms[0]
                for (m in ms) if (m.word.text.length > longest.word.text.length) longest = m
                val strokes = ms.map { Weight.measure(it.ink) }.filter { it > 0f }.sorted()
                val stroke = if (strokes.isEmpty()) 0f else strokes[strokes.size / 2]
                out.add(TextItem(nextId++, text, text, lineBox, longest.ink.color, font, line = li, stroke = stroke))
            } else {
                for (m in ms) {
                    out.add(TextItem(nextId++, m.word.text, m.word.text, m.ink.box, m.ink.color, font, line = li, stroke = Weight.measure(m.ink)))
                }
            }
        }
        return out
    }

    // ---------------------------------------------------------------- Traits

    private class Seg(var a1: Int, var a2: Int, var b1: Int, var b2: Int)

    private fun detectRules(pix: PixelBuf, paper: Int, textRects: List<Rect>): List<RuleLine> {
        val w = pix.w
        val h = pix.h
        val mask = BooleanArray(w * h)
        for (i in mask.indices) mask[i] = Colors.dist(pix.px[i], paper) > 55

        val result = ArrayList<RuleLine>()
        val maxThick = max(12f, h * 0.005f)
        val maxSpread = max(60, (h * 0.03f).toInt())

        // Traits horizontaux : on balaie les lignes de pixels.
        val minLenH = max(48, (w * 0.04f).toInt())
        val groupsH = ArrayList<Seg>() // a = y1..y2, b = x1..x2
        var active = ArrayList<Seg>()
        for (y in 0 until h) {
            val next = ArrayList<Seg>()
            val base = y * w
            var x = 0
            while (x < w) {
                if (!mask[base + x]) {
                    x++
                    continue
                }
                val s = x
                var e = x
                var gap = 0
                var cx = x
                while (cx < w) {
                    if (mask[base + cx]) {
                        e = cx
                        gap = 0
                    } else {
                        gap++
                        if (gap > 2) break
                    }
                    cx++
                }
                x = max(cx, e + 1)
                if (e - s + 1 >= minLenH) {
                    var g: Seg? = null
                    for (a in active) {
                        val ov = min(a.b2, e) - max(a.b1, s)
                        if (ov > 0.8f * min(a.b2 - a.b1, e - s)) {
                            g = a
                            break
                        }
                    }
                    if (g != null) {
                        g.a2 = y
                        g.b1 = min(g.b1, s)
                        g.b2 = max(g.b2, e)
                        if (!next.contains(g)) next.add(g)
                    } else {
                        val ng = Seg(y, y, s, e)
                        groupsH.add(ng)
                        next.add(ng)
                    }
                }
            }
            active = next
        }
        for (g in groupsH) {
            if (g.a2 - g.a1 + 1 > maxSpread) continue
            val pr = profile(mask, w, h, g, true) ?: continue
            val thick = max(1, pr[1].toInt())
            val len = g.b2 - g.b1 + 1
            if (pr[1] > maxThick || pr[2] < 0.5f || len < thick * 6) continue
            val cy = pr[0]
            if (cy < h * 0.006f || cy > h * 0.994f) continue
            if (insideText(textRects, (g.b1 + g.b2) / 2f, cy, vertical = false)) continue
            val color = averageColor(pix, mask, g.b1, g.b2, cy.toInt().coerceIn(0, h - 1), true)
            result.add(RuleLine(g.b1.toFloat(), cy, (g.b2 + 1).toFloat(), cy, pr[1], color))
        }

        // Traits verticaux : on balaie les colonnes de pixels.
        val minLenV = max(48, (h * 0.03f).toInt())
        val groupsV = ArrayList<Seg>() // a = x1..x2, b = y1..y2
        var activeV = ArrayList<Seg>()
        for (x in 0 until w) {
            val next = ArrayList<Seg>()
            var y = 0
            while (y < h) {
                if (!mask[y * w + x]) {
                    y++
                    continue
                }
                val s = y
                var e = y
                var gap = 0
                var cy = y
                while (cy < h) {
                    if (mask[cy * w + x]) {
                        e = cy
                        gap = 0
                    } else {
                        gap++
                        if (gap > 2) break
                    }
                    cy++
                }
                y = max(cy, e + 1)
                if (e - s + 1 >= minLenV) {
                    var g: Seg? = null
                    for (a in activeV) {
                        val ov = min(a.b2, e) - max(a.b1, s)
                        if (ov > 0.8f * min(a.b2 - a.b1, e - s)) {
                            g = a
                            break
                        }
                    }
                    if (g != null) {
                        g.a2 = x
                        g.b1 = min(g.b1, s)
                        g.b2 = max(g.b2, e)
                        if (!next.contains(g)) next.add(g)
                    } else {
                        val ng = Seg(x, x, s, e)
                        groupsV.add(ng)
                        next.add(ng)
                    }
                }
            }
            activeV = next
        }
        for (g in groupsV) {
            if (g.a2 - g.a1 + 1 > maxSpread) continue
            val pr = profile(mask, w, h, g, false) ?: continue
            val thick = max(1, pr[1].toInt())
            val len = g.b2 - g.b1 + 1
            if (pr[1] > maxThick || pr[2] < 0.5f || len < thick * 6) continue
            val cx = pr[0]
            if (cx < w * 0.006f || cx > w * 0.994f) continue
            if (insideText(textRects, cx, (g.b1 + g.b2) / 2f, vertical = true)) continue
            val color = averageColor(pix, mask, g.b1, g.b2, cx.toInt().coerceIn(0, w - 1), false)
            result.add(RuleLine(cx, g.b1.toFloat(), cx, (g.b2 + 1).toFloat(), pr[1], color))
        }
        return result
    }

    /**
     * Géométrie robuste d'un groupe de segments : position (médiane des centres colonne par colonne),
     * épaisseur (médiane des épaisseurs réelles) et part de la longueur réellement couverte.
     * Un papier plié décale la ligne d'un point à l'autre : la médiane l'ignore, au lieu d'épaissir le trait.
     */
    private fun profile(mask: BooleanArray, w: Int, h: Int, g: Seg, horizontal: Boolean): FloatArray? {
        val centers = ArrayList<Float>()
        val thicks = ArrayList<Int>()
        val n = g.b2 - g.b1 + 1
        for (t in g.b1..g.b2) {
            var bestRun = 0
            var bestStart = -1
            var run = 0
            var start = -1
            for (u in g.a1..g.a2) {
                val on = if (horizontal) mask[u * w + t] else mask[t * w + u]
                if (on) {
                    if (run == 0) start = u
                    run++
                    if (run > bestRun) {
                        bestRun = run
                        bestStart = start
                    }
                } else {
                    run = 0
                }
            }
            if (bestRun > 0) {
                centers.add(bestStart + bestRun / 2f)
                thicks.add(bestRun)
            }
        }
        if (centers.isEmpty() || n <= 0) return null
        centers.sort()
        thicks.sort()
        return floatArrayOf(centers[centers.size / 2], thicks[thicks.size / 2].toFloat(), centers.size.toFloat() / n)
    }

    /** Un « trait » qui traverse le cœur d'un mot est en réalité une lettre. */
    private fun insideText(rects: List<Rect>, cx: Float, cy: Float, vertical: Boolean): Boolean {
        for (r in rects) {
            val mx = r.width() * 0.15f
            val my = r.height() * 0.2f
            if (cx > r.left + mx && cx < r.right - mx && cy > r.top + my && cy < r.bottom - my) return true
        }
        return false
    }

    private fun averageColor(pix: PixelBuf, mask: BooleanArray, from: Int, to: Int, fixed: Int, horizontal: Boolean): Int {
        var sr = 0L
        var sg = 0L
        var sb = 0L
        var n = 0
        for (t in from..to) {
            val idx = if (horizontal) fixed * pix.w + t else t * pix.w + fixed
            if (mask[idx]) {
                val c = pix.px[idx]
                sr += Colors.red(c)
                sg += Colors.green(c)
                sb += Colors.blue(c)
                n++
            }
        }
        if (n == 0) return Color.BLACK
        val c = Colors.rgb((sr / n).toInt(), (sg / n).toInt(), (sb / n).toInt())
        return if (Colors.lum(c) < 100 && Colors.chroma(c) < 40) Color.BLACK else c
    }

    // ---------------------------------------------------------------- Aplats de couleur

    private fun detectFills(pix: PixelBuf, paper: Int): List<FillRect> {
        val bs = 8
        val bw = pix.w / bs
        val bh = pix.h / bs
        if (bw < 4 || bh < 4) return emptyList()
        val blockColor = IntArray(bw * bh)
        val flat = BooleanArray(bw * bh)
        val rs = IntArray(bs * bs)
        val gs = IntArray(bs * bs)
        val bsArr = IntArray(bs * bs)

        for (by in 0 until bh) {
            for (bx in 0 until bw) {
                var n = 0
                var mr = 0
                var mg = 0
                var mb = 0
                for (yy in 0 until bs) {
                    val row = (by * bs + yy) * pix.w + bx * bs
                    for (xx in 0 until bs) {
                        val c = pix.px[row + xx]
                        rs[n] = Colors.red(c)
                        gs[n] = Colors.green(c)
                        bsArr[n] = Colors.blue(c)
                        mr += rs[n]
                        mg += gs[n]
                        mb += bsArr[n]
                        n++
                    }
                }
                var cr = mr / n
                var cg = mg / n
                var cb = mb / n
                var inliers = 0
                for (iter in 0 until 2) {
                    var sr = 0
                    var sg = 0
                    var sb = 0
                    inliers = 0
                    for (k in 0 until n) {
                        val d = (abs(rs[k] - cr) + abs(gs[k] - cg) + abs(bsArr[k] - cb)) / 3
                        if (d <= 24) {
                            sr += rs[k]
                            sg += gs[k]
                            sb += bsArr[k]
                            inliers++
                        }
                    }
                    if (inliers == 0) break
                    cr = sr / inliers
                    cg = sg / inliers
                    cb = sb / inliers
                }
                if (inliers >= 38) {
                    val col = Colors.rgb(cr, cg, cb)
                    if (Colors.dist(col, paper) >= 40) {
                        flat[by * bw + bx] = true
                        blockColor[by * bw + bx] = col
                    }
                }
            }
        }

        // Composantes connexes de blocs de même couleur
        val visited = BooleanArray(bw * bh)
        val queue = IntArray(bw * bh)
        val result = ArrayList<FillRect>()
        for (start in 0 until bw * bh) {
            if (!flat[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var minX = bw
            var minY = bh
            var maxX = -1
            var maxY = -1
            var count = 0
            var sr = 0L
            var sg = 0L
            var sb = 0L
            while (head < tail) {
                val cur = queue[head++]
                val cx = cur % bw
                val cy = cur / bw
                count++
                minX = min(minX, cx)
                maxX = max(maxX, cx)
                minY = min(minY, cy)
                maxY = max(maxY, cy)
                val c = blockColor[cur]
                sr += Colors.red(c)
                sg += Colors.green(c)
                sb += Colors.blue(c)
                for (d in 0 until 4) {
                    val nx = cx + (if (d == 0) 1 else if (d == 1) -1 else 0)
                    val ny = cy + (if (d == 2) 1 else if (d == 3) -1 else 0)
                    if (nx < 0 || ny < 0 || nx >= bw || ny >= bh) continue
                    val ni = ny * bw + nx
                    if (!flat[ni] || visited[ni]) continue
                    if (Colors.dist(blockColor[ni], c) > 20) continue
                    visited[ni] = true
                    queue[tail++] = ni
                }
            }
            val bboxBlocks = (maxX - minX + 1) * (maxY - minY + 1)
            if (count < 12) continue
            if (count.toFloat() / bboxBlocks < 0.5f) continue
            val color = Colors.rgb((sr / count).toInt(), (sg / count).toInt(), (sb / count).toInt())
            val rect = refineRect(pix, color, minX * bs, minY * bs, (maxX + 1) * bs, (maxY + 1) * bs, bs)
            result.add(FillRect(rect, color))
        }
        result.sortByDescending { it.rect.width() * it.rect.height() }
        return if (result.size > 60) result.subList(0, 60).toList() else result
    }

    /** Affine les bords d'un aplat au pixel près (la grille de blocs est de 8 px). */
    private fun refineRect(pix: PixelBuf, color: Int, l: Int, t: Int, r: Int, b: Int, margin: Int): RectF {
        fun matches(x: Int, y: Int): Boolean = Colors.dist(pix.px[y * pix.w + x], color) < 26

        fun colFrac(x: Int): Float {
            if (x < 0 || x >= pix.w) return 0f
            val y0 = t + (b - t) / 10
            val y1 = b - (b - t) / 10
            var n = 0
            var ok = 0
            var y = y0
            while (y < y1) {
                if (matches(x, min(pix.h - 1, y))) ok++
                n++
                y += 2
            }
            return if (n == 0) 0f else ok.toFloat() / n
        }

        fun rowFrac(y: Int): Float {
            if (y < 0 || y >= pix.h) return 0f
            val x0 = l + (r - l) / 10
            val x1 = r - (r - l) / 10
            var n = 0
            var ok = 0
            var x = x0
            while (x < x1) {
                if (matches(min(pix.w - 1, x), y)) ok++
                n++
                x += 2
            }
            return if (n == 0) 0f else ok.toFloat() / n
        }

        var left = l
        for (x in (l - margin)..(l + margin)) {
            if (colFrac(x) >= 0.5f) {
                left = x
                break
            }
        }
        var right = r
        for (x in (r + margin) downTo (r - margin)) {
            if (colFrac(x - 1) >= 0.5f) {
                right = x
                break
            }
        }
        var top = t
        for (y in (t - margin)..(t + margin)) {
            if (rowFrac(y) >= 0.5f) {
                top = y
                break
            }
        }
        var bottom = b
        for (y in (b + margin) downTo (b - margin)) {
            if (rowFrac(y - 1) >= 0.5f) {
                bottom = y
                break
            }
        }
        return RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    // ---------------------------------------------------------------- Tampons (suggestions)

    /** Repère les grandes zones d'encre colorée (tampons) : proposées à l'utilisateur, jamais imposées. */
    private fun detectStamps(pix: PixelBuf): List<RectF> {
        val cell = 6
        val gw = pix.w / cell
        val gh = pix.h / cell
        if (gw < 10 || gh < 10) return emptyList()
        val flag = BooleanArray(gw * gh)
        for (gy in 0 until gh) {
            for (gx in 0 until gw) {
                var n = 0
                for (yy in 0 until cell) {
                    val row = (gy * cell + yy) * pix.w + gx * cell
                    for (xx in 0 until cell) {
                        val c = pix.px[row + xx]
                        val mx = max(Colors.red(c), max(Colors.green(c), Colors.blue(c)))
                        val mn = min(Colors.red(c), min(Colors.green(c), Colors.blue(c)))
                        if (mx > 60 && mx - mn > 50 && (mx - mn) > 0.45f * mx) n++
                    }
                }
                flag[gy * gw + gx] = n >= 5
            }
        }
        // Dilatation 5x5
        val dil = BooleanArray(gw * gh)
        for (gy in 0 until gh) {
            for (gx in 0 until gw) {
                var any = false
                var dy = -2
                while (dy <= 2 && !any) {
                    var dx = -2
                    while (dx <= 2) {
                        val nx = gx + dx
                        val ny = gy + dy
                        if (nx in 0 until gw && ny in 0 until gh && flag[ny * gw + nx]) {
                            any = true
                            break
                        }
                        dx++
                    }
                    dy++
                }
                dil[gy * gw + gx] = any
            }
        }
        val seen = BooleanArray(gw * gh)
        val queue = IntArray(gw * gh)
        val found = ArrayList<RectF>()
        for (start in 0 until gw * gh) {
            if (!dil[start] || seen[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            seen[start] = true
            var minX = gw
            var minY = gh
            var maxX = -1
            var maxY = -1
            var flagged = 0
            while (head < tail) {
                val cur = queue[head++]
                val cx = cur % gw
                val cy = cur / gw
                if (flag[cur]) flagged++
                minX = min(minX, cx)
                maxX = max(maxX, cx)
                minY = min(minY, cy)
                maxY = max(maxY, cy)
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx < 0 || ny < 0 || nx >= gw || ny >= gh) continue
                    val ni = ny * gw + nx
                    if (dil[ni] && !seen[ni]) {
                        seen[ni] = true
                        queue[tail++] = ni
                    }
                }
            }
            val wPx = (maxX - minX + 1) * cell
            val hPx = (maxY - minY + 1) * cell
            val density = flagged.toFloat() / ((maxX - minX + 1) * (maxY - minY + 1))
            if (wPx >= pix.w * 0.07f && hPx >= pix.w * 0.04f && wPx <= pix.w * 0.6f && density in 0.03f..0.55f) {
                val m = cell * 2
                found.add(
                    RectF(
                        max(0, minX * cell - m).toFloat(),
                        max(0, minY * cell - m).toFloat(),
                        min(pix.w, (maxX + 1) * cell + m).toFloat(),
                        min(pix.h, (maxY + 1) * cell + m).toFloat()
                    )
                )
            }
        }
        found.sortByDescending { it.width() * it.height() }
        return if (found.size > 4) found.subList(0, 4).toList() else found
    }
}
