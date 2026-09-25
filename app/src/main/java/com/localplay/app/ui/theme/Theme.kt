package com.localplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFC3C44), onPrimary = Color.White,
    background = Color.Black, surface = Color(0xFF1C1C1E),
    onBackground = Color.White, onSurface = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93), outline = Color(0xFF38383A)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFFFC3C44), onPrimary = Color.White,
    background = Color.White, surface = Color(0xFFF2F2F7),
    onBackground = Color.Black, onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF6C6C70), outline = Color(0xFFC6C6C8)
)

@Composable
fun LocalPlayTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = LocalPlayTypography,
        content     = content
    )
}
