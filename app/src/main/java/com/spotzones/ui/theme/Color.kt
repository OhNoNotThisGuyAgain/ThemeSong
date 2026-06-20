package com.spotzones.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SpotZones palette.
 *
 * The brand is anchored on Spotify's green so the companion feels native to it, paired with a
 * true-black (AMOLED) dark surface family and a clean, low-chroma light family. Tonal values are
 * hand-tuned rather than generated so contrast ratios meet WCAG AA against their on-colors.
 */
internal object Palette {
    val SpotifyGreen = Color(0xFF1DB954)
    val SpotifyGreenBright = Color(0xFF1ED760)
    val SpotifyGreenDark = Color(0xFF14833B)

    // Secondary — a calm teal that reads as "ambient / location".
    val Teal = Color(0xFF3DD5C8)
    val TealDark = Color(0xFF0E6E66)

    // Tertiary — warm amber for schedules / time-of-day accents.
    val Amber = Color(0xFFFFB951)
    val AmberDark = Color(0xFF7A5212)

    val Error = Color(0xFFFF5449)
    val ErrorDark = Color(0xFF93000A)

    // AMOLED neutrals.
    val Black = Color(0xFF000000)
    val Surface0 = Color(0xFF0B0B0F)
    val Surface1 = Color(0xFF121218)
    val Surface2 = Color(0xFF1A1A22)
    val Surface3 = Color(0xFF22222C)
    val OutlineDark = Color(0xFF44444F)
    val OnSurfaceDark = Color(0xFFEAEAF0)
    val OnSurfaceVariantDark = Color(0xFFB9B9C6)

    // Light neutrals.
    val White = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFFBFBFE)
    val LightSurfaceVariant = Color(0xFFE2E2EC)
    val OutlineLight = Color(0xFF75757F)
    val OnSurfaceLight = Color(0xFF1A1B1F)
    val OnSurfaceVariantLight = Color(0xFF45464C)
}

/**
 * Default accent options offered in Settings. Stored as an enum-like id so the choice survives
 * across versions even if the exact color is later retuned.
 */
enum class AccentColor(val id: String, val seed: Color) {
    Spotify("spotify", Palette.SpotifyGreen),
    Teal("teal", Palette.Teal),
    Amber("amber", Palette.Amber),
    Violet("violet", Color(0xFF8B7CFF)),
    Rose("rose", Color(0xFFFF6F9C));

    companion object {
        fun fromId(id: String?): AccentColor = entries.firstOrNull { it.id == id } ?: Spotify
    }
}
