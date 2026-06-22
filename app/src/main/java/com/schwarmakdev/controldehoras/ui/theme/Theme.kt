package com.schwarmakdev.controldehoras.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    // El esquema se construye DENTRO del composable para que los getters de color
    // (que dependen de ThemeState.isDark y currentColorTheme) se evalúen en cada
    // recomposición. Si se definieran como `val` de nivel superior quedarían
    // congelados con los valores del tema oscuro por defecto.
    val colorScheme = if (ThemeState.isDark) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            primary = PrimaryEmerald,
            secondary = SecondaryMint,
            tertiary = AccentedTeal,
            background = DarkBackground,   // los getters resuelven al valor claro
            surface = DarkSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = TextCrispWhite,
            onSurface = TextCrispWhite,
            surfaceVariant = PanelBlue,
            onSurfaceVariant = TextCrispWhite
        )
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
