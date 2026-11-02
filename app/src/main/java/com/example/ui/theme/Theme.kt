package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JimmyDarkColorScheme = darkColorScheme(
    primary = JimmyOrangeBright,
    onPrimary = JimmyTextPrimary,
    primaryContainer = JimmyOrangeDeep,
    onPrimaryContainer = JimmyTextPrimary,
    secondary = JimmyBlueAccent,
    onSecondary = JimmyTextPrimary,
    secondaryContainer = JimmyNavyCard,
    onSecondaryContainer = JimmyTextSecondary,
    tertiary = JimmyGoldCrown,
    background = JimmyNavyDarkest,
    onBackground = JimmyTextPrimary,
    surface = JimmyNavyDeep,
    onSurface = JimmyTextPrimary,
    surfaceVariant = JimmyNavyCard,
    onSurfaceVariant = JimmyTextSecondary,
    outline = JimmyNavyBorder,
    error = JimmyDangerRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JimmyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
