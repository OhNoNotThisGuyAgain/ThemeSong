package com.spotzones.data.remote.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the in-flight PKCE verifier and OAuth `state` across the Custom Tab round-trip. Persisted
 * (not just in-memory) so an authorization survives the app process being killed while the browser
 * is foregrounded. Cleared as soon as it is consumed or the flow is abandoned.
 */
@Singleton
class PendingPkceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("pkce_pending", Context.MODE_PRIVATE)

    fun start(codes: PkceCodes, state: String) {
        prefs.edit {
            putString(KEY_VERIFIER, codes.verifier)
            putString(KEY_STATE, state)
        }
    }

    fun expectedState(): String? = prefs.getString(KEY_STATE, null)

    /** Returns the verifier exactly once, then clears it. */
    fun consume(): String? {
        val verifier = prefs.getString(KEY_VERIFIER, null)
        prefs.edit { clear() }
        return verifier
    }

    fun abandon() = prefs.edit { clear() }

    private companion object {
        const val KEY_VERIFIER = "verifier"
        const val KEY_STATE = "state"
    }
}
