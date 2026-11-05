package com.fidelpdf.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

/** Forme de l'icône (repère 108 x 108, identique à l'icône de l'application). */
internal class LogoShape(val pts: FloatArray, val color: Long, val alpha: Float)

internal val LogoShapes = listOf(
    LogoShape(floatArrayOf(34.50f, 32.50f, 58.50f, 32.50f, 70.50f, 44.50f, 70.50f, 82.50f, 70.36f, 83.54f, 69.96f, 84.50f, 69.33f, 85.33f, 68.50f, 85.96f, 67.54f, 86.36f, 66.50f, 86.50f, 34.50f, 86.50f, 33.46f, 86.36f, 32.50f, 85.96f, 31.67f, 85.33f, 31.04f, 84.50f, 30.64f, 83.54f, 30.50f, 82.50f, 30.50f, 36.50f, 30.64f, 35.46f, 31.04f, 34.50f, 31.67f, 33.67f, 32.50f, 33.04f, 33.46f, 32.64f, 34.50f, 32.50f), 0xFF000000, 0.2f),
    LogoShape(floatArrayOf(33.00f, 30.00f, 57.00f, 30.00f, 69.00f, 42.00f, 69.00f, 80.00f, 68.86f, 81.04f, 68.46f, 82.00f, 67.83f, 82.83f, 67.00f, 83.46f, 66.04f, 83.86f, 65.00f, 84.00f, 33.00f, 84.00f, 31.96f, 83.86f, 31.00f, 83.46f, 30.17f, 82.83f, 29.54f, 82.00f, 29.14f, 81.04f, 29.00f, 80.00f, 29.00f, 34.00f, 29.14f, 32.96f, 29.54f, 32.00f, 30.17f, 31.17f, 31.00f, 30.54f, 31.96f, 30.14f, 33.00f, 30.00f), 0xFFFFFFFF, 1.0f),
    LogoShape(floatArrayOf(57.00f, 30.00f, 69.00f, 42.00f, 63.00f, 42.00f, 59.50f, 41.50f, 58.00f, 40.00f, 57.00f, 36.00f), 0xFFCFD1FF, 1.0f),
    LogoShape(floatArrayOf(60.50f, 50.00f, 60.89f, 50.05f, 61.25f, 50.20f, 61.56f, 50.44f, 61.80f, 50.75f, 61.95f, 51.11f, 62.00f, 51.50f, 62.00f, 51.50f, 61.95f, 51.89f, 61.80f, 52.25f, 61.56f, 52.56f, 61.25f, 52.80f, 60.89f, 52.95f, 60.50f, 53.00f, 37.50f, 53.00f, 37.11f, 52.95f, 36.75f, 52.80f, 36.44f, 52.56f, 36.20f, 52.25f, 36.05f, 51.89f, 36.00f, 51.50f, 36.00f, 51.50f, 36.05f, 51.11f, 36.20f, 50.75f, 36.44f, 50.44f, 36.75f, 50.20f, 37.11f, 50.05f, 37.50f, 50.00f), 0xFF4F46E5, 1.0f),
    LogoShape(floatArrayOf(60.50f, 57.00f, 60.89f, 57.05f, 61.25f, 57.20f, 61.56f, 57.44f, 61.80f, 57.75f, 61.95f, 58.11f, 62.00f, 58.50f, 62.00f, 58.50f, 61.95f, 58.89f, 61.80f, 59.25f, 61.56f, 59.56f, 61.25f, 59.80f, 60.89f, 59.95f, 60.50f, 60.00f, 37.50f, 60.00f, 37.11f, 59.95f, 36.75f, 59.80f, 36.44f, 59.56f, 36.20f, 59.25f, 36.05f, 58.89f, 36.00f, 58.50f, 36.00f, 58.50f, 36.05f, 58.11f, 36.20f, 57.75f, 36.44f, 57.44f, 36.75f, 57.20f, 37.11f, 57.05f, 37.50f, 57.00f), 0xFF4F46E5, 1.0f),
    LogoShape(floatArrayOf(51.50f, 64.00f, 51.89f, 64.05f, 52.25f, 64.20f, 52.56f, 64.44f, 52.80f, 64.75f, 52.95f, 65.11f, 53.00f, 65.50f, 53.00f, 65.50f, 52.95f, 65.89f, 52.80f, 66.25f, 52.56f, 66.56f, 52.25f, 66.80f, 51.89f, 66.95f, 51.50f, 67.00f, 37.50f, 67.00f, 37.11f, 66.95f, 36.75f, 66.80f, 36.44f, 66.56f, 36.20f, 66.25f, 36.05f, 65.89f, 36.00f, 65.50f, 36.00f, 65.50f, 36.05f, 65.11f, 36.20f, 64.75f, 36.44f, 64.44f, 36.75f, 64.20f, 37.11f, 64.05f, 37.50f, 64.00f), 0xFF4F46E5, 1.0f),
    LogoShape(floatArrayOf(53.50f, 71.00f, 53.89f, 71.05f, 54.25f, 71.20f, 54.56f, 71.44f, 54.80f, 71.75f, 54.95f, 72.11f, 55.00f, 72.50f, 55.00f, 72.50f, 54.95f, 72.89f, 54.80f, 73.25f, 54.56f, 73.56f, 54.25f, 73.80f, 53.89f, 73.95f, 53.50f, 74.00f, 37.50f, 74.00f, 37.11f, 73.95f, 36.75f, 73.80f, 36.44f, 73.56f, 36.20f, 73.25f, 36.05f, 72.89f, 36.00f, 72.50f, 36.00f, 72.50f, 36.05f, 72.11f, 36.20f, 71.75f, 36.44f, 71.44f, 36.75f, 71.20f, 37.11f, 71.05f, 37.50f, 71.00f), 0xFF14B8A6, 1.0f),
    LogoShape(floatArrayOf(20.00f, 54.00f, 88.00f, 54.00f, 88.00f, 66.00f, 20.00f, 66.00f), 0xFF2DD4BF, 0.2f),
    LogoShape(floatArrayOf(86.30f, 58.00f, 86.74f, 58.06f, 87.15f, 58.23f, 87.50f, 58.50f, 87.77f, 58.85f, 87.94f, 59.26f, 88.00f, 59.70f, 88.00f, 59.70f, 87.94f, 60.14f, 87.77f, 60.55f, 87.50f, 60.90f, 87.15f, 61.17f, 86.74f, 61.34f, 86.30f, 61.40f, 21.70f, 61.40f, 21.26f, 61.34f, 20.85f, 61.17f, 20.50f, 60.90f, 20.23f, 60.55f, 20.06f, 60.14f, 20.00f, 59.70f, 20.00f, 59.70f, 20.06f, 59.26f, 20.23f, 58.85f, 20.50f, 58.50f, 20.85f, 58.23f, 21.26f, 58.06f, 21.70f, 58.00f), 0xFF5EEAD4, 0.95f),
    LogoShape(floatArrayOf(82.20f, 38.00f, 82.10f, 39.59f, 81.78f, 41.16f, 81.27f, 42.67f, 80.57f, 44.10f, 79.68f, 45.43f, 78.63f, 46.63f, 77.43f, 47.68f, 76.10f, 48.57f, 74.67f, 49.27f, 73.16f, 49.78f, 71.59f, 50.10f, 70.00f, 50.20f, 68.41f, 50.10f, 66.84f, 49.78f, 65.33f, 49.27f, 63.90f, 48.57f, 62.57f, 47.68f, 61.37f, 46.63f, 60.32f, 45.43f, 59.43f, 44.10f, 58.73f, 42.67f, 58.22f, 41.16f, 57.90f, 39.59f, 57.80f, 38.00f, 57.90f, 36.41f, 58.22f, 34.84f, 58.73f, 33.33f, 59.43f, 31.90f, 60.32f, 30.57f, 61.37f, 29.37f, 62.57f, 28.32f, 63.90f, 27.43f, 65.33f, 26.73f, 66.84f, 26.22f, 68.41f, 25.90f, 70.00f, 25.80f, 71.59f, 25.90f, 73.16f, 26.22f, 74.67f, 26.73f, 76.10f, 27.43f, 77.43f, 28.32f, 78.63f, 29.37f, 79.68f, 30.57f, 80.57f, 31.90f, 81.27f, 33.33f, 81.78f, 34.84f, 82.10f, 36.41f), 0xFFFFFFFF, 1.0f),
    LogoShape(floatArrayOf(80.40f, 38.00f, 80.31f, 39.36f, 80.05f, 40.69f, 79.61f, 41.98f, 79.01f, 43.20f, 78.25f, 44.33f, 77.35f, 45.35f, 76.33f, 46.25f, 75.20f, 47.01f, 73.98f, 47.61f, 72.69f, 48.05f, 71.36f, 48.31f, 70.00f, 48.40f, 68.64f, 48.31f, 67.31f, 48.05f, 66.02f, 47.61f, 64.80f, 47.01f, 63.67f, 46.25f, 62.65f, 45.35f, 61.75f, 44.33f, 60.99f, 43.20f, 60.39f, 41.98f, 59.95f, 40.69f, 59.69f, 39.36f, 59.60f, 38.00f, 59.69f, 36.64f, 59.95f, 35.31f, 60.39f, 34.02f, 60.99f, 32.80f, 61.75f, 31.67f, 62.65f, 30.65f, 63.67f, 29.75f, 64.80f, 28.99f, 66.02f, 28.39f, 67.31f, 27.95f, 68.64f, 27.69f, 70.00f, 27.60f, 71.36f, 27.69f, 72.69f, 27.95f, 73.98f, 28.39f, 75.20f, 28.99f, 76.33f, 29.75f, 77.35f, 30.65f, 78.25f, 31.67f, 79.01f, 32.80f, 79.61f, 34.02f, 80.05f, 35.31f, 80.31f, 36.64f), 0xFFF59E0B, 1.0f),
    LogoShape(floatArrayOf(66.40f, 32.20f, 69.00f, 32.20f, 69.00f, 44.00f, 66.40f, 44.00f), 0xFF1E1B6E, 1.0f),
    LogoShape(floatArrayOf(66.40f, 32.20f, 74.80f, 32.20f, 74.80f, 34.80f, 66.40f, 34.80f), 0xFF1E1B6E, 1.0f),
    LogoShape(floatArrayOf(66.40f, 37.20f, 72.60f, 37.20f, 72.60f, 39.60f, 66.40f, 39.60f), 0xFF1E1B6E, 1.0f)
)

/** Logo FidelPDF dessiné en vectoriel : même dessin que l'icône de l'application. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF3B2FD9), Color(0xFF7C3AED), Color(0xFF14B8A6)),
                Offset.Zero,
                Offset(s, s)
            ),
            cornerRadius = CornerRadius(s * 0.22f)
        )
        val k = s / 80f
        for (shape in LogoShapes) {
            val path = Path()
            var i = 0
            while (i < shape.pts.size) {
                val x = (shape.pts[i] - 54f) * k + s / 2f
                val y = (shape.pts[i + 1] - 54f) * k + s / 2f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                i += 2
            }
            path.close()
            drawPath(path, Color(shape.color), alpha = shape.alpha)
        }
    }
}
