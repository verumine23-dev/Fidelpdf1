package com.fidelpdf.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidelpdf.app.AppViewModel
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Recadrage de la photo avant la reconstruction : on ne garde que le document utile. */
@Composable
fun CropScreen(vm: AppViewModel) {
    val src = vm.cropSource
    if (src == null) {
        LaunchedEffect(Unit) { vm.cancelCrop() }
        return
    }
    val img = remember(src) { src.asImageBitmap() }
    // quatre coins : hautG, hautD, basD, basG (x, y en fractions de l'image)
    val q = remember(src) {
        mutableStateListOf(0.03f, 0.03f, 0.97f, 0.03f, 0.97f, 0.97f, 0.03f, 0.97f)
    }
    val proposal = vm.cropQuad
    LaunchedEffect(proposal) {
        if (proposal != null) for (i in 0 until 8) q[i] = proposal[i]
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.cancelCrop() }) { Icon(Icons.Rounded.Close, "Annuler", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("Recadrer le document", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Placez les 4 coins sur les coins de la feuille : elle sera redressée",
                    color = Color(0xFFB9BBD6),
                    fontSize = 12.sp
                )
            }
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()
            val scale = min(cw / src.width, ch / src.height)
            val dw = src.width * scale
            val dh = src.height * scale
            val ox = (cw - dw) / 2f
            val oy = (ch - dh) / 2f

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(src, cw, ch) {
                        var corner = -1
                        var moveAll = false
                        val hit = 56.dp.toPx()
                        detectDragGestures(
                            onDragStart = { o ->
                                corner = -1
                                moveAll = false
                                var bestD = hit
                                for (i in 0 until 4) {
                                    val px = ox + q[i * 2] * dw
                                    val py = oy + q[i * 2 + 1] * dh
                                    val d = hypot(o.x - px, o.y - py)
                                    if (d < bestD) {
                                        bestD = d
                                        corner = i
                                    }
                                }
                                if (corner < 0) moveAll = true
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val dx = drag.x / dw
                                val dy = drag.y / dh
                                if (corner >= 0) {
                                    q[corner * 2] = (q[corner * 2] + dx).coerceIn(0f, 1f)
                                    q[corner * 2 + 1] = (q[corner * 2 + 1] + dy).coerceIn(0f, 1f)
                                } else if (moveAll) {
                                    var mdx = dx
                                    var mdy = dy
                                    for (i in 0 until 4) {
                                        mdx = if (dx >= 0) min(mdx, 1f - q[i * 2]) else max(mdx, -q[i * 2])
                                        mdy = if (dy >= 0) min(mdy, 1f - q[i * 2 + 1]) else max(mdy, -q[i * 2 + 1])
                                    }
                                    for (i in 0 until 4) {
                                        q[i * 2] = q[i * 2] + mdx
                                        q[i * 2 + 1] = q[i * 2 + 1] + mdy
                                    }
                                }
                            }
                        )
                    }
            ) {
                drawImage(
                    image = img,
                    dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                    dstSize = IntSize(max(1, dw.toInt()), max(1, dh.toInt()))
                )
                val pts = List(4) { Offset(ox + q[it * 2] * dw, oy + q[it * 2 + 1] * dh) }
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(ox, oy, ox + dw, oy + dh))
                    moveTo(pts[0].x, pts[0].y)
                    lineTo(pts[1].x, pts[1].y)
                    lineTo(pts[2].x, pts[2].y)
                    lineTo(pts[3].x, pts[3].y)
                    close()
                }
                drawPath(path, Color(0xA6000000))
                val outline = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    lineTo(pts[1].x, pts[1].y)
                    lineTo(pts[2].x, pts[2].y)
                    lineTo(pts[3].x, pts[3].y)
                    close()
                }
                drawPath(outline, Color.White, style = Stroke(width = 2.5f))
                // repères au tiers, suivant la perspective
                val guide = Color(0x66FFFFFF)
                for (k in 1..2) {
                    val f = k / 3f
                    val a = Offset(pts[0].x + (pts[1].x - pts[0].x) * f, pts[0].y + (pts[1].y - pts[0].y) * f)
                    val b = Offset(pts[3].x + (pts[2].x - pts[3].x) * f, pts[3].y + (pts[2].y - pts[3].y) * f)
                    drawLine(guide, a, b, strokeWidth = 1.2f)
                    val c = Offset(pts[0].x + (pts[3].x - pts[0].x) * f, pts[0].y + (pts[3].y - pts[0].y) * f)
                    val d = Offset(pts[1].x + (pts[2].x - pts[1].x) * f, pts[1].y + (pts[2].y - pts[1].y) * f)
                    drawLine(guide, c, d, strokeWidth = 1.2f)
                }
                // poignées
                for (p in pts) {
                    drawCircle(Color.White, radius = 14.dp.toPx(), center = p)
                    drawCircle(Indigo, radius = 10.dp.toPx(), center = p)
                    drawCircle(Color.White, radius = 3.5.dp.toPx(), center = p)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { vm.rotateCrop(-90f) }) { Icon(Icons.Rounded.RotateLeft, "Pivoter à gauche", tint = Color.White) }
            TextButton(onClick = { vm.detectCrop() }) {
                Icon(Icons.Rounded.Crop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Détecter", color = Color.White)
            }
            TextButton(onClick = {
                val full = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                for (i in 0 until 8) q[i] = full[i]
            }) { Text("Tout", color = Color.White) }
            IconButton(onClick = { vm.rotateCrop(90f) }) { Icon(Icons.Rounded.RotateRight, "Pivoter à droite", tint = Color.White) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { vm.cancelCrop() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Annuler", color = Color.White) }
            Button(
                onClick = { vm.confirmCrop(FloatArray(8) { q[it] }) },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Scanner ce cadre", fontWeight = FontWeight.SemiBold) }
        }
    }
}
