package com.spotzones.data.remote.auth

import com.spotzones.BuildConfig
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.remote.api.SpotifyAuthApiService
import com.spotzones.data.security.SecureTokenStore
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Owns the OAuth token lifecycle for the Web API.
 *
 * - Tokens live only in [SecureTokenStore] (encrypted). This class never logs them.
 * - [validAccessToken] refreshes transparently when within [REFRESH_SKEW_MS] of expiry, guarded by a
 *   mutex so concurrent callers trigger at most one refresh.
 * - All failures map to a [DomainError] so the UI can react (re-authorize, show offline, etc.).
 */
@Singleton
class SpotifyAuthImpl @Inject constructor(
    private val authApi: SpotifyAuthApiService,
    private val tokenStore: SecureTokenStore,
    private val pendingPkce: PendingPkceStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SpotifyAuth {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow(currentState())
    override val state: StateFlow<SpotifyAuthState> = _state.asStateFlow()

    override suspend fun validAccessToken(): Outcome<String> = withContext(io) {
        val access = tokenStore.accessToken
        val expiresAt = tokenStore.expiresAtEpochMs
        when {
            access == null -> Outcome.Failure(DomainError.SpotifyNotAuthorized)
            System.currentTimeMillis() < expiresAt - REFRESH_SKEW_MS -> Outcome.Success(access)
            else -> refresh()
        }
    }

    override suspend fun onAuthorizationResult(code: String): Outcome<Unit> = withContext(io) {
        val verifier = pendingPkce.consume()
            ?: return@withContext Outcome.Failure(DomainError.Validation("Authorization session expired. Try again."))
        try {
            val token = authApi.exchangeToken(
                grantType = "authorization_code",
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = verifier,
            )
            persist(token.accessToken, token.refreshToken, token.expiresIn)
            _state.value = currentState()
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(DomainError.NoNetwork)
        } catch (e: Exception) {
            Timber.w("Token exchange failed")
            Outcome.Failure(DomainError.SpotifyRemote("Couldn't complete Spotify sign-in."))
        }
    }

    override suspend fun logout() = withContext(io) {
        tokenStore.clear()
        _state.value = SpotifyAuthState.Unauthorized
    }

    private suspend fun refresh(): Outcome<String> = refreshMutex.withLock {
        // Re-check after acquiring the lock: another caller may have refreshed already.
        val freshExpiry = tokenStore.expiresAtEpochMs
        val current = tokenStore.accessToken
        if (current != null && System.currentTimeMillis() < freshExpiry - REFRESH_SKEW_MS) {
            return@withLock Outcome.Success(current)
        }
        val refreshToken = tokenStore.refreshToken
            ?: return@withLock Outcome.Failure(DomainError.SpotifyNotAuthorized)
        try {
            val token = authApi.exchangeToken(
                grantType = "refresh_token",
                clientId = clientId,
                refreshToken = refreshToken,
            )
            // Spotify may or may not rotate the refresh token; keep the old one if absent.
            persist(token.accessToken, token.refreshToken ?: refreshToken, token.expiresIn)
            _state.value = currentState()
            Outcome.Success(token.accessToken)
        } catch (e: IOException) {
            Outcome.Failure(DomainError.NoNetwork)
        } catch (e: Exception) {
            _state.value = SpotifyAuthState.Expired
            Outcome.Failure(DomainError.SpotifyNotAuthorized)
        }
    }

    private fun persist(access: String, refresh: String?, expiresInSeconds: Long) {
        tokenStore.accessToken = access
        if (refresh != null) tokenStore.refreshToken = refresh
        tokenStore.expiresAtEpochMs = System.currentTimeMillis() + expiresInSeconds * 1000
    }

    private fun currentState(): SpotifyAuthState {
        val access = tokenStore.accessToken ?: return SpotifyAuthState.Unauthorized
        val expiresAt = tokenStore.expiresAtEpochMs
        return if (System.currentTimeMillis() >= expiresAt && tokenStore.refreshToken == null) {
            SpotifyAuthState.Expired
        } else {
            SpotifyAuthState.Authorized(tokenStore.displayName, expiresAt)
        }
    }

    private companion object {
        const val REFRESH_SKEW_MS = 60_000L
    }
}
