package com.spotzones.data.remote.auth

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.spotzones.BuildConfig
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the interactive authorization step: generates PKCE codes, opens a Chrome Custom Tab to the
 * Spotify consent screen, and validates + exchanges the redirect. Keeping this out of the ViewModel
 * means the OAuth mechanics are unit-coverable and the UI just calls two methods.
 */
@Singleton
class SpotifyAuthCoordinator @Inject constructor(
    private val pendingPkce: PendingPkceStore,
    private val auth: SpotifyAuth,
) {
    val isConfigured: Boolean get() = BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank()

    /** Builds the consent-screen intent and persists the matching PKCE verifier + state. */
    fun buildAuthIntent(): Intent {
        val codes = PkceFactory.generate()
        val state = UUID.randomUUID().toString()
        pendingPkce.start(codes, state)
        val uri = PkceFactory.authorizeUrl(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            challenge = codes.challenge,
            scopes = PkceFactory.REQUIRED_SCOPES,
            state = state,
        )
        return CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .intent
            .setData(uri)
    }

    /** Returns true if [uri] is our OAuth redirect (so the activity knows to consume it). */
    fun isRedirect(uri: Uri?): Boolean =
        uri != null && uri.scheme == Uri.parse(BuildConfig.SPOTIFY_REDIRECT_URI).scheme

    suspend fun handleRedirect(uri: Uri): Outcome<Unit> {
        val error = uri.getQueryParameter("error")
        if (error != null) {
            pendingPkce.abandon()
            return Outcome.Failure(DomainError.Validation("Spotify sign-in was cancelled."))
        }
        val returnedState = uri.getQueryParameter("state")
        if (returnedState == null || returnedState != pendingPkce.expectedState()) {
            pendingPkce.abandon()
            return Outcome.Failure(DomainError.Validation("Sign-in could not be verified. Please try again."))
        }
        val code = uri.getQueryParameter("code")
            ?: return Outcome.Failure(DomainError.Validation("Missing authorization code."))
        return auth.onAuthorizationResult(code)
    }
}
