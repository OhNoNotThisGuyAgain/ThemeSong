package com.spotzones.domain.spotify

import com.spotzones.domain.util.Outcome
import kotlinx.coroutines.flow.StateFlow

/** Authorization state for the Spotify Web API (App Remote uses its own session). */
sealed interface SpotifyAuthState {
    data object Unauthorized : SpotifyAuthState
    data class Authorized(val displayName: String?, val expiresAt: Long) : SpotifyAuthState
    data object Expired : SpotifyAuthState
}

/**
 * Owns the OAuth token lifecycle. Tokens are stored encrypted (see data layer) and refreshed
 * transparently. The UI only needs [state] and the [authorize]/[logout] entry points.
 */
interface SpotifyAuth {
    val state: StateFlow<SpotifyAuthState>

    /** Returns a valid access token, refreshing if necessary, or a failure the UI can surface. */
    suspend fun validAccessToken(): Outcome<String>

    /** Persists the result of an interactive authorization (auth code / token). */
    suspend fun onAuthorizationResult(code: String): Outcome<Unit>

    suspend fun logout()
}
