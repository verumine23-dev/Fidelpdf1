package com.fidelpdf.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visionneuse : pincer pour zoomer / dézoomer, glisser pour se déplacer,
 * double appui pour passer du plein cadre au zoom et inversement.
 * Les calques sont superposés (par exemple le scan puis la carte des écarts).
 */
@Composable
fun ZoomImage(layers: List<ImageBitmap>, modifier: Modifier = Modifier, hint: Boolean = true) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE3E5F0))
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { p ->
                    if (scale > 1.05f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        val ns = 3f
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val o = p - (p - offset) * (ns / scale)
                        scale = ns
                        offset = Offset(o.x.coerceIn(w - w * ns, 0f), o.y.coerceIn(h - h * ns, 0f))
                    }
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val ns = (scale * zoom).coerceIn(1f, 8f)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val o = centroid - (centroid - offset) * (ns / scale) + pan
                    scale = ns
                    offset = Offset(o.x.coerceIn(w - w * ns, 0f), o.y.coerceIn(h - h * ns, 0f))
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            for (img in layers) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                )
            }
        }
        if (hint) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xB3000000))
            ) {
                Text(
                    if (scale > 1.05f) "Zoom ×" + String.format("%.1f", scale) + " · double appui : tout voir"
                    else "Pincez pour zoomer · double appui : agrandir",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}
