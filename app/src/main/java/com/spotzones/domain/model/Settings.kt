package com.spotzones.domain.model

/** Geofence responsiveness vs. battery trade-off, mapped to dwell delays and location intervals. */
enum class GeofenceSensitivity { BATTERY_SAVER, BALANCED, RESPONSIVE }

/** Theme preference persisted via DataStore. Mirrors the UI [com.spotzones.ui.theme.ThemeMode]. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/**
 * All user-tunable preferences, persisted in DataStore. A single immutable object keeps reads
 * consistent and makes the settings screen a pure function of state.
 */
data class AppSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val accentColorId: String = "spotify",
    val amoledBlack: Boolean = true,
    val dynamicColor: Boolean = false,
    val automationEnabled: Boolean = true,
    val geofenceSensitivity: GeofenceSensitivity = GeofenceSensitivity.BALANCED,
    val showTransitionNotifications: Boolean = true,
    val defaultCrossfadeSeconds: Int = 6,
    val defaultVolumePercent: Int = 80,
    val requireHeadphonesForAuto: Boolean = false,
    val pauseAutomationWhileOnCall: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
) {
    companion object {
        val Default = AppSettings()
    }
}

/** Active manual override that temporarily suspends automation. */
data class ManualOverride(
    val mode: OverrideMode,
    /** Epoch millis at which the override lapses; null for indefinite / event-bound modes. */
    val expiresAt: Long?,
    /** For [OverrideMode.UNTIL_LEAVE_ZONE], the zone whose exit clears the override. */
    val boundZoneId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun isActive(now: Long, currentZoneId: String?): Boolean = when (mode) {
        OverrideMode.PERMANENT -> true
        OverrideMode.UNTIL_LEAVE_ZONE -> currentZoneId != null && currentZoneId == boundZoneId
        else -> expiresAt != null && now < expiresAt
    }
}

enum class OverrideMode { FIFTEEN_MIN, THIRTY_MIN, ONE_HOUR, UNTIL_LEAVE_ZONE, UNTIL_TOMORROW, PERMANENT }
