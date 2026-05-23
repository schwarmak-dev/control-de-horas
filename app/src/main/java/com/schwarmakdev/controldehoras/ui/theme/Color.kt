package com.schwarmakdev.controldehoras.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

enum class AppThemeColor {
    MINT_CYAN,
    PURPLE,
    BLUE,
    FOREST_GREEN,
    YELLOW,
    ELECTRIC_BLUE
}

object ThemeState {
    var isDark by mutableStateOf(true)
    var currentColorTheme by mutableStateOf(AppThemeColor.MINT_CYAN)
}

val DarkBackground: Color
    get() = if (ThemeState.isDark) Color(0xFF0B1017) else Color(0xFFF9FAFB)

val DarkSurface: Color
    get() = if (ThemeState.isDark) Color(0xFF161E2E) else Color(0xFFFFFFFF)

val PrimaryEmerald: Color
    get() = when (ThemeState.currentColorTheme) {
        AppThemeColor.MINT_CYAN     -> if (ThemeState.isDark) Color(0xFF10B981) else Color(0xFF059669)
        AppThemeColor.PURPLE        -> if (ThemeState.isDark) Color(0xFF8B5CF6) else Color(0xFF6D28D9)
        AppThemeColor.BLUE          -> if (ThemeState.isDark) Color(0xFF3B82F6) else Color(0xFF1D4ED8)
        AppThemeColor.FOREST_GREEN  -> if (ThemeState.isDark) Color(0xFF22C55E) else Color(0xFF15803D)
        AppThemeColor.YELLOW        -> if (ThemeState.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
        AppThemeColor.ELECTRIC_BLUE -> if (ThemeState.isDark) Color(0xFF00D2FF) else Color(0xFF007A9B)
    }

val SecondaryMint: Color
    get() = when (ThemeState.currentColorTheme) {
        AppThemeColor.MINT_CYAN     -> if (ThemeState.isDark) Color(0xFF34D399) else Color(0xFF10B981)
        AppThemeColor.PURPLE        -> if (ThemeState.isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6)
        AppThemeColor.BLUE          -> if (ThemeState.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        AppThemeColor.FOREST_GREEN  -> if (ThemeState.isDark) Color(0xFF4ADE80) else Color(0xFF22C55E)
        AppThemeColor.YELLOW        -> if (ThemeState.isDark) Color(0xFFFCD34D) else Color(0xFFFBBF24)
        AppThemeColor.ELECTRIC_BLUE -> if (ThemeState.isDark) Color(0xFF80E5FF) else Color(0xFF00D2FF)
    }

val AccentedTeal: Color
    get() = when (ThemeState.currentColorTheme) {
        AppThemeColor.MINT_CYAN     -> if (ThemeState.isDark) Color(0xFF0D9488) else Color(0xFF0F766E)
        AppThemeColor.PURPLE        -> if (ThemeState.isDark) Color(0xFF7C3AED) else Color(0xFF5B21B6)
        AppThemeColor.BLUE          -> if (ThemeState.isDark) Color(0xFF2563EB) else Color(0xFF1E40AF)
        AppThemeColor.FOREST_GREEN  -> if (ThemeState.isDark) Color(0xFF15803D) else Color(0xFF14532D)
        AppThemeColor.YELLOW        -> if (ThemeState.isDark) Color(0xFFD97706) else Color(0xFF78350F)
        AppThemeColor.ELECTRIC_BLUE -> if (ThemeState.isDark) Color(0xFF0891B2) else Color(0xFF0B5394)
    }

val TextCrispWhite: Color
    get() = if (ThemeState.isDark) Color(0xFFF9FAFB) else Color(0xFF111827)

val TextSubtleGray: Color
    get() = if (ThemeState.isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)

val AlertRed: Color
    get() = if (ThemeState.isDark) Color(0xFFEF4444) else Color(0xFFB3261E)

val PanelBlue: Color
    get() = if (ThemeState.isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6)

val LightAlertBackground: Color
    get() = if (ThemeState.isDark) Color(0xFF2D1E22) else Color(0xFFFEE2E2)

val AlertBorder: Color
    get() = if (ThemeState.isDark) Color(0xFF5F2124) else Color(0xFFFCA5A5)

val SolidWhite: Color
    get() = if (ThemeState.isDark) Color(0xFF111827) else Color(0xFFFFFFFF)

val ContentBorder: Color
    get() = if (ThemeState.isDark) Color(0xFF374151) else Color(0xFFE5E7EB)

val ButtonContentColor: Color
    get() = if (ThemeState.currentColorTheme == AppThemeColor.YELLOW) {
        Color.Black
    } else {
        if (ThemeState.isDark) Color.Black else Color.White
    }
