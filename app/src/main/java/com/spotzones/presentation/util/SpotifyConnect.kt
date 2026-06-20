package com.spotzones.presentation.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult

/**
 * Launches the Spotify PKCE consent screen in a Chrome Custom Tab (in-app browser chrome).
 * Redirect back to SpotZones is handled by [com.spotzones.presentation.MainActivity].
 */
fun launchSpotifyConnect(
    context: Context,
    buildIntent: () -> Intent,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null,
) {
    try {
        val intent = buildIntent()
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            context.startActivity(intent)
        }
    } catch (_: IllegalStateException) {
        Toast.makeText(context, "Spotify sign-in isn't available in this build.", Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't open Spotify sign-in.", Toast.LENGTH_LONG).show()
    }
}
