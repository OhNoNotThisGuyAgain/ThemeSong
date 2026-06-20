package com.spotzones.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable theme mode, persisted in settings. */
enum class ThemeMode { System, Light, Dark }

private fun darkScheme(accent: Color, amoled: Boolean): ColorScheme {
    val background = if (amoled) Palette.Black else Palette.Surface0
    return darkColorScheme(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.5f) Palette.Black else Palette.White,
        primaryContainer = accent.copy(alpha = 0.22f).compositeOverBlack(),
        onPrimaryContainer = accent.lighten(0.35f),
        secondary = Palette.Teal,
        onSecondary = Palette.Black,
        secondaryContainer = Palette.TealDark,
        onSecondaryContainer = Palette.Teal.lighten(0.3f),
        tertiary = Palette.Amber,
        onTertiary = Palette.Black,
        tertiaryContainer = Palette.AmberDark,
        onTertiaryContainer = Palette.Amber.lighten(0.25f),
        error = Palette.Error,
        onError = Palette.Black,
        errorContainer = Palette.ErrorDark,
        onErrorContainer = Palette.Error.lighten(0.3f),
        background = background,
        onBackground = Palette.OnSurfaceDark,
        surface = background,
        onSurface = Palette.OnSurfaceDark,
        surfaceVariant = Palette.Surface2,
        onSurfaceVariant = Palette.OnSurfaceVariantDark,
        surfaceContainerLowest = if (amoled) Palette.Black else Palette.Surface0,
        surfaceContainerLow = Palette.Surface1,
        surfaceContainer = Palette.Surface2,
        surfaceContainerHigh = Palette.Surface3,
        surfaceContainerHighest = Palette.Surface3.lighten(0.04f),
        outline = Palette.OutlineDark,
        outlineVariant = Palette.Surface3,
        inverseSurface = Palette.OnSurfaceDark,
        inverseOnSurface = Palette.Surface1,
        inversePrimary = accent.darken(0.25f),
        scrim = Palette.Black,
    )
}

private fun lightScheme(accent: Color): ColorScheme = lightColorScheme(
    primary = accent.darken(0.1f),
    onPrimary = Palette.White,
    primaryContainer = accent.lighten(0.7f),
    onPrimaryContainer = accent.darken(0.55f),
    secondary = Palette.TealDark,
    onSecondary = Palette.White,
    tertiary = Palette.AmberDark,
    onTertiary = Palette.White,
    error = Palette.Error,
    onError = Palette.White,
    background = Palette.LightSurface,
    onBackground = Palette.OnSurfaceLight,
    surface = Palette.LightSurface,
    onSurface = Palette.OnSurfaceLight,
    surfaceVariant = Palette.LightSurfaceVariant,
    onSurfaceVariant = Palette.OnSurfaceVariantLight,
    outline = Palette.OutlineLight,
)

/**
 * Root theme.
 *
 * @param dynamicColor honour Material You wallpaper colors on Android 12+. Off by default so the
 *   curated brand palette wins, but exposed as a setting.
 * @param amoled collapse dark surfaces to true black to save power on OLED panels.
 */
@Composable
fun SpotZonesTheme(
    themeMode: ThemeMode = ThemeMode.System,
    accent: AccentColor = AccentColor.Spotify,
    dynamicColor: Boolean = false,
    amoled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent.seed, amoled)
        else -> lightScheme(accent.seed)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpotZonesTypography,
        shapes = SpotZonesShapes,
        content = content,
    )
}

// --- Small color helpers (kept private to the theme package) ---

private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)

private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha,
)

private fun Color.compositeOverBlack(): Color = Color(
    red = red * alpha,
    green = green * alpha,
    blue = blue * alpha,
    alpha = 1f,
)
