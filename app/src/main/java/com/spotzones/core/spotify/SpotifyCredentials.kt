package com.spotzones.core.spotify

import android.content.Context
import com.spotzones.BuildConfig
import com.spotzones.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the SpotZones Spotify Developer app credentials baked into this build.
 *
 * End users never configure a Client ID — the app publisher registers one dashboard app
 * (see docs/SPOTIFY_SETUP.md) and embeds its Client ID here. PKCE means the Client ID is
 * not secret; it is safe to ship in the APK.
 */
@Singleton
class SpotifyCredentials @Inject constructor(
    @ApplicationContext context: Context,
) {
    val clientId: String = sequenceOf(
        BuildConfig.SPOTIFY_CLIENT_ID,
        runCatching { context.getString(R.string.spotify_client_id) }.getOrDefault(""),
    ).firstOrNull { it.isNotBlank() }.orEmpty()

    val redirectUri: String = BuildConfig.SPOTIFY_REDIRECT_URI

    val isAvailable: Boolean get() = clientId.isNotBlank()
}
