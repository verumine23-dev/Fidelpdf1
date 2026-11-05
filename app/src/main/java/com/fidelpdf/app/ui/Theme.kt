package com.fidelpdf.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Indigo = Color(0xFF4F46E5)
val IndigoDeep = Color(0xFF1E1B6E)
val Navy = Color(0xFF0B0D24)
val Teal = Color(0xFF14B8A6)
val Violet = Color(0xFF7C3AED)
val Amber = Color(0xFFF59E0B)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2FF),
    onPrimaryContainer = Color(0xFF14106B),
    secondary = Color(0xFF0E9F9B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5F1),
    onSecondaryContainer = Color(0xFF003835),
    tertiary = Amber,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF14152B),
    surface = Color.White,
    onSurface = Color(0xFF14152B),
    surfaceVariant = Color(0xFFE9EAF7),
    onSurfaceVariant = Color(0xFF454766),
    outline = Color(0xFFB9BBD6)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9A9BFF),
    onPrimary = Color(0xFF1B1585),
    primaryContainer = Color(0xFF2E2AA0),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFF4FD8CF),
    onSecondary = Color(0xFF003835),
    secondaryContainer = Color(0xFF00504C),
    onSecondaryContainer = Color(0xFFCFF5F1),
    tertiary = Color(0xFFFFC857),
    background = Color(0xFF0E1020),
    onBackground = Color(0xFFE6E7F6),
    surface = Color(0xFF171A33),
    onSurface = Color(0xFFE6E7F6),
    surfaceVariant = Color(0xFF23274A),
    onSurfaceVariant = Color(0xFFC3C5E3),
    outline = Color(0xFF5B5F8A)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun FidelTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        shapes = AppShapes,
        content = content
    )
}
