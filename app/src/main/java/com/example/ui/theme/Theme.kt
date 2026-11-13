package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkOmniPalette.accentCyan,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF003644),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = DarkOmniPalette.accentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381559),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = LuxuryGold,
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF5A4400),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = DarkOmniPalette.background,
    onBackground = DarkOmniPalette.textPrimary,
    surface = DarkOmniPalette.surface,
    onSurface = DarkOmniPalette.textPrimary,
    surfaceVariant = DarkOmniPalette.surfaceElevated,
    onSurfaceVariant = DarkOmniPalette.textSecondary,
    outline = DarkOmniPalette.border,
    outlineVariant = DarkOmniPalette.borderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = LightOmniPalette.accentCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = LightOmniPalette.accentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = LuxuryGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = LightOmniPalette.background,
    onBackground = LightOmniPalette.textPrimary,
    surface = LightOmniPalette.surface,
    onSurface = LightOmniPalette.textPrimary,
    surfaceVariant = LightOmniPalette.surfaceElevated,
    onSurfaceVariant = LightOmniPalette.textSecondary,
    outline = LightOmniPalette.border,
    outlineVariant = LightOmniPalette.borderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkOmniPalette else LightOmniPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOmniPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
