package com.schwarmakdev.controldehoras.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    secondary = SecondaryMint,
    tertiary = AccentedTeal,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextCrispWhite,
    onSurface = TextCrispWhite,
    surfaceVariant = PanelBlue,
    onSurfaceVariant = TextCrispWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    secondary = SecondaryMint,
    tertiary = AccentedTeal,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextCrispWhite,
    onSurface = TextCrispWhite,
    surfaceVariant = PanelBlue,
    onSurfaceVariant = TextCrispWhite
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val colorScheme = if (ThemeState.isDark) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
