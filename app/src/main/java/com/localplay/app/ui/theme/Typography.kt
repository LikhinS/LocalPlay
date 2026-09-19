package com.localplay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Apple Music-matching typography scale.
 * Uses the system font (Roboto on Android) which reads cleanly
 * on a 720p A21s display and costs zero extra APK size.
 */
val LocalPlayTypography = Typography(
    // Large album/artist titles
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 17.sp,
        lineHeight = 22.sp
    ),
    // Section headers ("Songs", "Albums", etc.)
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 17.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        lineHeight = 20.sp
    ),
    // Track title in list rows
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 17.sp,
        lineHeight = 22.sp
    ),
    // Artist / subtitle in list rows
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 20.sp
    ),
    // Duration, badge labels
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp
    )
)
