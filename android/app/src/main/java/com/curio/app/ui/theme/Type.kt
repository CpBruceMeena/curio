package com.curio.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val HankenGrotesk = FontFamily.Default // Will use system sans-serif as fallback until font file is added

val CurioTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 56.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 61.6.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.4.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 33.6.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 31.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 28.8.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.6.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 14.sp,
        letterSpacing = 0.05.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 11.sp,
        letterSpacing = 0.05.sp
    )
)
