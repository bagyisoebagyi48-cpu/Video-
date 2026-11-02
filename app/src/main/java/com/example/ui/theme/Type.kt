package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    )
)

val PlusJakartaFontFamily = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        loadingStrategy = FontLoadingStrategy.OptionalLocal
    )
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
