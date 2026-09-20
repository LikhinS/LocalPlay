package com.localplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppleRed            = Color(0xFFFC3C44)
val SurfaceDark         = Color(0xFF1C1C1E)
val SurfaceLight        = Color(0xFFF2F2F7)
val SecondaryLabelDark  = Color(0xFF8E8E93)
val SecondaryLabelLight = Color(0xFF6C6C70)
val SeparatorDark       = Color(0xFF38383A)
val SeparatorLight      = Color(0xFFC6C6C8)

private val DarkColors = darkColorScheme(
    primary = AppleRed, onPrimary = Color.White,
    background = Color.Black, surface = SurfaceDark,
    onBackground = Color.White, onSurface = Color.White,
    onSurfaceVariant = SecondaryLabelDark, outline = SeparatorDark
)
private val LightColors = lightColorScheme(
    primary = AppleRed, onPrimary = Color.White,
    background = Color.White, surface = SurfaceLight,
    onBackground = Color.Black, onSurface = Color.Black,
    onSurfaceVariant = SecondaryLabelLight, outline = SeparatorLight
)

@Composable
fun LocalPlayTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = LocalPlayTypography,
        content     = content
    )
}
