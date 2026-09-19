package com.localplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Apple Music color palette
val AppleRed       = Color(0xFFFC3C44)
val ApplePink      = Color(0xFFFF375F)
val SurfaceDark    = Color(0xFF1C1C1E)
val SurfaceLight   = Color(0xFFF2F2F7)
val BackgroundDark = Color(0xFF000000)
val BackgroundLight= Color(0xFFFFFFFF)
val LabelDark      = Color(0xFFFFFFFF)
val LabelLight     = Color(0xFF000000)
val SecondaryLabelDark  = Color(0xFF8E8E93)
val SecondaryLabelLight = Color(0xFF6C6C70)
val SeparatorDark  = Color(0xFF38383A)
val SeparatorLight = Color(0xFFC6C6C8)

private val DarkColors = darkColorScheme(
    primary          = AppleRed,
    onPrimary        = Color.White,
    secondary        = ApplePink,
    background       = BackgroundDark,
    surface          = SurfaceDark,
    onBackground     = LabelDark,
    onSurface        = LabelDark,
    onSurfaceVariant = SecondaryLabelDark,
    outline          = SeparatorDark,
)

private val LightColors = lightColorScheme(
    primary          = AppleRed,
    onPrimary        = Color.White,
    secondary        = ApplePink,
    background       = BackgroundLight,
    surface          = SurfaceLight,
    onBackground     = LabelLight,
    onSurface        = LabelLight,
    onSurfaceVariant = SecondaryLabelLight,
    outline          = SeparatorLight,
)

@Composable
fun LocalPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = LocalPlayTypography,
        content     = content
    )
}
