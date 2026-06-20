package com.spotzones.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.model.GeofenceSensitivity
import com.spotzones.domain.model.ManualOverride
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.model.ThemePreference
import com.spotzones.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Preferences DataStore implementation. Each field maps to a typed key, so reads are total (missing
 * keys fall back to [AppSettings.Default]) and writes are atomic transformations — no partial state.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val amoled = booleanPreferencesKey("amoled")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val automationEnabled = booleanPreferencesKey("automation_enabled")
        val sensitivity = stringPreferencesKey("geofence_sensitivity")
        val transitionNotifs = booleanPreferencesKey("transition_notifs")
        val crossfade = intPreferencesKey("default_crossfade")
        val volume = intPreferencesKey("default_volume")
        val requireHeadphones = booleanPreferencesKey("require_headphones")
        val pauseOnCall = booleanPreferencesKey("pause_on_call")
        val analytics = booleanPreferencesKey("analytics")
        val onboarding = booleanPreferencesKey("onboarding_complete")

        val overrideMode = stringPreferencesKey("override_mode")
        val overrideExpiresAt = stringPreferencesKey("override_expires_at")
        val overrideZone = stringPreferencesKey("override_zone")
        val overrideCreatedAt = stringPreferencesKey("override_created_at")
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.theme] = updated.themePreference.name
            prefs[Keys.accent] = updated.accentColorId
            prefs[Keys.amoled] = updated.amoledBlack
            prefs[Keys.dynamicColor] = updated.dynamicColor
            prefs[Keys.automationEnabled] = updated.automationEnabled
            prefs[Keys.sensitivity] = updated.geofenceSensitivity.name
            prefs[Keys.transitionNotifs] = updated.showTransitionNotifications
            prefs[Keys.crossfade] = updated.defaultCrossfadeSeconds
            prefs[Keys.volume] = updated.defaultVolumePercent
            prefs[Keys.requireHeadphones] = updated.requireHeadphonesForAuto
            prefs[Keys.pauseOnCall] = updated.pauseAutomationWhileOnCall
            prefs[Keys.analytics] = updated.analyticsEnabled
            prefs[Keys.onboarding] = updated.onboardingComplete
        }
    }

    override val override: Flow<ManualOverride?> = dataStore.data.map { it.toOverride() }

    override suspend fun currentOverride(): ManualOverride? = override.first()

    override suspend fun setOverride(override: ManualOverride?) {
        dataStore.edit { prefs ->
            if (override == null) {
                prefs.remove(Keys.overrideMode)
                prefs.remove(Keys.overrideExpiresAt)
                prefs.remove(Keys.overrideZone)
                prefs.remove(Keys.overrideCreatedAt)
            } else {
                prefs[Keys.overrideMode] = override.mode.name
                prefs[Keys.overrideExpiresAt] = override.expiresAt?.toString() ?: ""
                prefs[Keys.overrideZone] = override.boundZoneId ?: ""
                prefs[Keys.overrideCreatedAt] = override.createdAt.toString()
            }
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themePreference = this[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: AppSettings.Default.themePreference,
        accentColorId = this[Keys.accent] ?: AppSettings.Default.accentColorId,
        amoledBlack = this[Keys.amoled] ?: AppSettings.Default.amoledBlack,
        dynamicColor = this[Keys.dynamicColor] ?: AppSettings.Default.dynamicColor,
        automationEnabled = this[Keys.automationEnabled] ?: AppSettings.Default.automationEnabled,
        geofenceSensitivity = this[Keys.sensitivity]?.let { runCatching { GeofenceSensitivity.valueOf(it) }.getOrNull() }
            ?: AppSettings.Default.geofenceSensitivity,
        showTransitionNotifications = this[Keys.transitionNotifs] ?: AppSettings.Default.showTransitionNotifications,
        defaultCrossfadeSeconds = this[Keys.crossfade] ?: AppSettings.Default.defaultCrossfadeSeconds,
        defaultVolumePercent = this[Keys.volume] ?: AppSettings.Default.defaultVolumePercent,
        requireHeadphonesForAuto = this[Keys.requireHeadphones] ?: AppSettings.Default.requireHeadphonesForAuto,
        pauseAutomationWhileOnCall = this[Keys.pauseOnCall] ?: AppSettings.Default.pauseAutomationWhileOnCall,
        analyticsEnabled = this[Keys.analytics] ?: AppSettings.Default.analyticsEnabled,
        onboardingComplete = this[Keys.onboarding] ?: AppSettings.Default.onboardingComplete,
    )

    private fun Preferences.toOverride(): ManualOverride? {
        val mode = this[Keys.overrideMode]?.let { runCatching { OverrideMode.valueOf(it) }.getOrNull() } ?: return null
        return ManualOverride(
            mode = mode,
            expiresAt = this[Keys.overrideExpiresAt]?.toLongOrNull(),
            boundZoneId = this[Keys.overrideZone]?.takeIf { it.isNotEmpty() },
            createdAt = this[Keys.overrideCreatedAt]?.toLongOrNull() ?: System.currentTimeMillis(),
        )
    }
}
