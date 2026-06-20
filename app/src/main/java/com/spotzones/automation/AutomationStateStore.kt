package com.spotzones.automation

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tiny persisted record of what the automation engine last did, so transitions are computed as a
 * diff (only act when the winning candidate actually changes). Surviving process death here prevents
 * re-applying the same playlist after the OS restarts the app on a geofence event.
 */
@Singleton
class AutomationStateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("automation_state", Context.MODE_PRIVATE)

    private val _activeCandidateId = MutableStateFlow(prefs.getString(KEY_ACTIVE_ID, null))
    val activeCandidateId: StateFlow<String?> = _activeCandidateId

    var lastActiveZoneId: String?
        get() = prefs.getString(KEY_ACTIVE_ZONE, null)
        private set(value) = prefs.edit { putString(KEY_ACTIVE_ZONE, value) }

    fun update(candidateId: String?, zoneId: String?) {
        prefs.edit {
            putString(KEY_ACTIVE_ID, candidateId)
            putString(KEY_ACTIVE_ZONE, zoneId)
        }
        _activeCandidateId.value = candidateId
    }

    private companion object {
        const val KEY_ACTIVE_ID = "active_candidate_id"
        const val KEY_ACTIVE_ZONE = "active_zone_id"
    }
}
