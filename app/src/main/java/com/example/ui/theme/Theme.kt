package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = LightBlueAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = DarkBlueText,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = LightBlueAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = DarkBlueText,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
