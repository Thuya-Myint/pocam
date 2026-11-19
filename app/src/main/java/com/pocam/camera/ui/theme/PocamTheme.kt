package com.pocam.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pocam.camera.ui.components.PocamPalette

/** Fixed studio colors: Pocam never changes accent with wallpaper or system mode. */
private val studioColors = darkColorScheme(
    primary = PocamPalette.Orange,
    onPrimary = PocamPalette.Black,
    primaryContainer = PocamPalette.Graphite,
    onPrimaryContainer = PocamPalette.OrangeSoft,
    secondary = PocamPalette.Silver,
    onSecondary = PocamPalette.Black,
    tertiary = PocamPalette.OrangeSoft,
    background = PocamPalette.Black,
    onBackground = Color.White,
    surface = PocamPalette.Carbon,
    onSurface = Color.White,
    surfaceVariant = PocamPalette.Graphite,
    onSurfaceVariant = PocamPalette.Silver,
    outline = PocamPalette.Steel,
    error = Color(0xFFDB563D)
)
@Composable
fun PocamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = studioColors, content = content)
}
