package com.spotzones.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector

/** Type-safe route constants. Args are encoded as path/query segments to keep nav state shareable. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val MAP = "map"
    const val NOW_PLAYING = "now_playing"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val ONBOARDING = "onboarding"

    const val ZONE_EDITOR = "zone_editor"
    const val ZONE_EDITOR_ARG_ID = "zoneId"
    const val ZONE_EDITOR_ARG_LAT = "lat"
    const val ZONE_EDITOR_ARG_LNG = "lng"

    /** @param zoneId existing zone to edit, or null to create. Optional lat/lng pre-seed the center. */
    fun zoneEditor(zoneId: String? = null, lat: Double? = null, lng: Double? = null): String {
        val id = zoneId ?: "new"
        val latArg = lat ?: Double.NaN
        val lngArg = lng ?: Double.NaN
        return "$ZONE_EDITOR/$id?$ZONE_EDITOR_ARG_LAT=$latArg&$ZONE_EDITOR_ARG_LNG=$lngArg"
    }

    const val ZONE_EDITOR_PATTERN =
        "$ZONE_EDITOR/{$ZONE_EDITOR_ARG_ID}?$ZONE_EDITOR_ARG_LAT={$ZONE_EDITOR_ARG_LAT}&$ZONE_EDITOR_ARG_LNG={$ZONE_EDITOR_ARG_LNG}"
}

/** Bottom-navigation top-level destinations. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard(Routes.DASHBOARD, "Home", Icons.Outlined.SpaceDashboard),
    Map(Routes.MAP, "Map", Icons.Outlined.Map),
    NowPlaying(Routes.NOW_PLAYING, "Playing", Icons.Outlined.MusicNote),
    History(Routes.HISTORY, "History", Icons.Outlined.History),
    Settings(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
}
