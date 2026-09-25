package com.localplay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.localplay.app.R

val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_light,    FontWeight.Light),
    Font(R.font.spacegrotesk_regular,  FontWeight.Normal),
    Font(R.font.spacegrotesk_medium,   FontWeight.Medium),
    Font(R.font.spacegrotesk_semibold, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold,     FontWeight.Bold)
)

val LocalPlayTypography = Typography(
    headlineLarge  = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,     fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall  = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleLarge     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium    = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium,   fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal,   fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light,    fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall     = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.5.sp)
)
