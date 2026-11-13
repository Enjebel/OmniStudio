package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class OmniColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val cardOverlay: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentPurple: Color,
    val accentPink: Color,
    val accentGold: Color,
    val timelineRulerBg: Color,
    val timelineRulerTick: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val glassCardBrush: Brush,
    val heroBannerBrush: Brush,
    val previewCardBrush: Brush
)

val DarkOmniPalette = OmniColorPalette(
    isDark = true,
    background = Color(0xFF0B0F19),         // Enterprise Dark Canvas (#0B0F19)
    surface = Color(0xFF111827),            // Modular Slate Card (#111827)
    surfaceElevated = Color(0xFF161E2E),    // Elevated Panel Surface
    surfaceHighlight = Color(0xFF1E293B),   // Interactive Slate Highlight (#1E293B)
    cardOverlay = Color(0xD9111827),
    border = Color(0xFF1E293B),             // Crisp 1px Slate Divider (#1E293B)
    borderSubtle = Color(0xFF172030),
    textPrimary = Color(0xFFF8FAFC),        // Slate 50
    textSecondary = Color(0xFF94A3B8),      // Slate 400
    textMuted = Color(0xFF64748B),          // Slate 500
    accentCyan = Color(0xFF0EA5E9),         // Crisp Pro Sky/Cyan
    accentPurple = Color(0xFF6366F1),       // Crisp Studio Indigo
    accentPink = Color(0xFFE11D48),         // Clean Rose Accent
    accentGold = Color(0xFFF59E0B),         // Refined Amber Gold
    timelineRulerBg = Color(0xFF0E1320),
    timelineRulerTick = Color(0xFF334155),
    errorContainer = Color(0xFF2B1218),
    onErrorContainer = Color(0xFFFECACA),
    glassCardBrush = Brush.verticalGradient(listOf(Color(0xFF131B2B), Color(0xFF111827))),
    heroBannerBrush = Brush.horizontalGradient(listOf(Color(0xFF151C2C), Color(0xFF111827))),
    previewCardBrush = Brush.linearGradient(listOf(Color(0xFF131B2E), Color(0xFF0F172A)))
)

val LightOmniPalette = OmniColorPalette(
    isDark = false,
    background = Color(0xFFF8FAFC),         // Clean Alabaster (#F8FAFC)
    surface = Color(0xFFFFFFFF),            // Pure White Cards (#FFFFFF)
    surfaceElevated = Color(0xFFF1F5F9),    // Slate 100
    surfaceHighlight = Color(0xFFE2E8F0),   // Slate 200
    cardOverlay = Color(0xF2FFFFFF),
    border = Color(0xFFE2E8F0),             // Crisp 1px Slate 200 Divider
    borderSubtle = Color(0xFFF1F5F9),
    textPrimary = Color(0xFF0F172A),        // Slate 900 (#0F172A)
    textSecondary = Color(0xFF475569),      // Slate 600
    textMuted = Color(0xFF64748B),          // Slate 500
    accentCyan = Color(0xFF0284C7),         // Deep Sky 600
    accentPurple = Color(0xFF4F46E5),       // Deep Indigo 600
    accentPink = Color(0xFFBE123C),         // Deep Rose 700
    accentGold = Color(0xFFB45309),         // Amber 700
    timelineRulerBg = Color(0xFFF1F5F9),
    timelineRulerTick = Color(0xFF94A3B8),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B),
    glassCardBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))),
    heroBannerBrush = Brush.horizontalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFFFFFFF))),
    previewCardBrush = Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
)

val LocalOmniPalette = staticCompositionLocalOf { DarkOmniPalette }

object OmniTheme {
    val colors: OmniColorPalette
        @Composable get() = LocalOmniPalette.current
}

// Adaptive Theme Tokens (automatically switch between Neutral Dark & Clean Light)
val StudioBackground: Color @Composable get() = LocalOmniPalette.current.background
val StudioSurface: Color @Composable get() = LocalOmniPalette.current.surface
val StudioSurfaceElevated: Color @Composable get() = LocalOmniPalette.current.surfaceElevated
val StudioSurfaceHighlight: Color @Composable get() = LocalOmniPalette.current.surfaceHighlight
val StudioCardOverlay: Color @Composable get() = LocalOmniPalette.current.cardOverlay

val StudioBorder: Color @Composable get() = LocalOmniPalette.current.border
val StudioBorderSubtle: Color @Composable get() = LocalOmniPalette.current.borderSubtle
val StudioBorderGlow: Color = Color(0xFF334155)
val StudioBorderActive: Color = Color(0xFF6366F1)

val TextPrimary: Color @Composable get() = LocalOmniPalette.current.textPrimary
val TextSecondary: Color @Composable get() = LocalOmniPalette.current.textSecondary
val TextMuted: Color @Composable get() = LocalOmniPalette.current.textMuted
val GlassCardBrush: Brush @Composable get() = LocalOmniPalette.current.glassCardBrush

// Disciplined Brand Accent Tokens (CapCut / Canva Pro grade)
val NeonPurple = Color(0xFF6366F1)          // Studio Indigo
val NeonCyan = Color(0xFF0EA5E9)            // Pro Sky Accent
val NeonPink = Color(0xFFE11D48)            // Editorial Crimson/Rose
val CyberGreen = Color(0xFF10B981)          // Emerald Verified
val DangerRed = Color(0xFFEF4444)

// Tier & Badge Accents
val LuxuryGold = Color(0xFFD97706)          // Refined Amber
val GoldGlow = Color(0xFFF59E0B)            // Amber Highlight
val GoldBackground = Color(0xFF271D07)

// Crisp Brand Action Brushes (clean two-stop enterprise gradients without neon wash)
val NeonGradient = Brush.horizontalGradient(
    listOf(Color(0xFF4F46E5), Color(0xFF0284C7))
)

val NeonTriGradient = Brush.horizontalGradient(
    listOf(Color(0xFF6366F1), Color(0xFF0EA5E9))
)

val NeonVerticalGradient = Brush.verticalGradient(
    listOf(Color(0xFF6366F1), Color(0xFF0EA5E9))
)

val GoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFF59E0B), Color(0xFFD97706))
)

// Static Material 3 Helpers
val OnPrimaryDark = Color(0xFFFFFFFF)
val OnSecondaryDark = Color(0xFFFFFFFF)
