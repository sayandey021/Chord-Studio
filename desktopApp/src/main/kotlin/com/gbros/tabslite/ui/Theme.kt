package com.gbros.tabslite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.gbros.tabslite.data.DesktopDatabase

data class AppThemeColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val borderHover: Color,
    val accent: Color,
    val textPrimary: Color = Color(0xFFF1F5F9),
    val textSecondary: Color = Color(0xFF94A3B8),
    val textMuted: Color = Color(0xFF64748B)
)

val LocalAppColors = staticCompositionLocalOf {
    AppThemeColors(
        bg = Color(0xFF0C0D12),
        surface = Color(0xFF101218),
        card = Color(0xFF151821),
        cardElevated = Color(0xFF1C202C),
        border = Color(0xFF1E222D),
        borderHover = Color(0xFF2C3244),
        accent = Color(0xFF3B82F6)
    )
}

val AppColors: AppThemeColors
    @Composable
    get() = LocalAppColors.current

// Dynamic theme colors returning active theme from LocalAppColors
val ColorObsidianBg: Color @Composable get() = LocalAppColors.current.bg
val ColorSurfaceDark: Color @Composable get() = LocalAppColors.current.surface
val ColorSurfaceCard: Color @Composable get() = LocalAppColors.current.card
val ColorSurfaceCardElevated: Color @Composable get() = LocalAppColors.current.cardElevated
val ColorCardBorder: Color @Composable get() = LocalAppColors.current.border
val ColorCardBorderHover: Color @Composable get() = LocalAppColors.current.borderHover
val ColorAccentBlue: Color @Composable get() = LocalAppColors.current.accent
val ColorTextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val ColorTextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val ColorTextMuted: Color @Composable get() = LocalAppColors.current.textMuted

@Composable
fun TabsLiteTheme(
    content: @Composable () -> Unit
) {
    val settings by DesktopDatabase.instance.settingsFlow.collectAsState()

    val accent = when (settings.accentColor) {
        "CYAN" -> Color(0xFF06B6D4)
        "GREEN" -> Color(0xFF10B981)
        "PURPLE" -> Color(0xFF8B5CF6)
        "AMBER" -> Color(0xFFF59E0B)
        "ROSE" -> Color(0xFFF43F5E)
        else -> Color(0xFF3B82F6) // BLUE default
    }

    val themeColors = when (settings.themeMode) {
        "MIDNIGHT_BLUE" -> AppThemeColors(
            bg = Color(0xFF070B16),
            surface = Color(0xFF0D1322),
            card = Color(0xFF121B30),
            cardElevated = Color(0xFF1A2642),
            border = Color(0xFF202E50),
            borderHover = Color(0xFF2E3F6D),
            accent = accent
        )
        "AMOLED_BLACK" -> AppThemeColors(
            bg = Color(0xFF000000),
            surface = Color(0xFF0A0A0A),
            card = Color(0xFF121212),
            cardElevated = Color(0xFF1C1C1C),
            border = Color(0xFF262626),
            borderHover = Color(0xFF383838),
            accent = accent
        )
        "NORD_SLATE" -> AppThemeColors(
            bg = Color(0xFF11141A),
            surface = Color(0xFF161A22),
            card = Color(0xFF1E232E),
            cardElevated = Color(0xFF262C3A),
            border = Color(0xFF2F3748),
            borderHover = Color(0xFF404B62),
            accent = accent
        )
        "WARM_EMBER" -> AppThemeColors(
            bg = Color(0xFF120E0C),
            surface = Color(0xFF191310),
            card = Color(0xFF221A16),
            cardElevated = Color(0xFF2D231E),
            border = Color(0xFF3B2E28),
            borderHover = Color(0xFF4E3D35),
            accent = accent
        )
        else -> AppThemeColors( // OBSIDIAN_DARK default
            bg = Color(0xFF0C0D12),
            surface = Color(0xFF101218),
            card = Color(0xFF151821),
            cardElevated = Color(0xFF1C202C),
            border = Color(0xFF1E222D),
            borderHover = Color(0xFF2C3244),
            accent = accent
        )
    }

    val materialColorScheme = darkColorScheme(
        primary = themeColors.accent,
        onPrimary = Color.White,
        primaryContainer = themeColors.cardElevated,
        onPrimaryContainer = themeColors.textPrimary,
        secondary = themeColors.accent,
        onSecondary = Color.White,
        background = themeColors.bg,
        onBackground = themeColors.textPrimary,
        surface = themeColors.surface,
        onSurface = themeColors.textPrimary,
        surfaceVariant = themeColors.card,
        onSurfaceVariant = themeColors.textSecondary,
        outline = themeColors.border,
        outlineVariant = themeColors.borderHover
    )

    CompositionLocalProvider(LocalAppColors provides themeColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
