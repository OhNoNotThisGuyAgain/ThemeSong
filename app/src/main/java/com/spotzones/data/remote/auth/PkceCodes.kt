package com.spotzones.data.remote.auth

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** A generated PKCE verifier/challenge pair. */
data class PkceCodes(val verifier: String, val challenge: String)

/**
 * Authorization Code with PKCE is the correct flow for a public (no client secret) mobile client,
 * so SpotZones never ships a Spotify client secret. The verifier is generated here, kept in memory
 * (and in encrypted prefs across the browser round-trip), and exchanged for tokens after redirect.
 */
object PkceFactory {

    fun generate(): PkceCodes {
        val verifier = randomVerifier()
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return PkceCodes(verifier, challenge)
    }

    fun authorizeUrl(
        clientId: String,
        redirectUri: String,
        challenge: String,
        scopes: List<String>,
        state: String,
    ): Uri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
        .appendQueryParameter("client_id", clientId)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", redirectUri)
        .appendQueryParameter("code_challenge_method", "S256")
        .appendQueryParameter("code_challenge", challenge)
        .appendQueryParameter("scope", scopes.joinToString(" "))
        .appendQueryParameter("state", state)
        .build()

    private fun randomVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /** Scopes required for playback control, library reads and search. */
    val REQUIRED_SCOPES = listOf(
        "user-read-private",
        "user-read-playback-state",
        "user-modify-playback-state",
        "playlist-read-private",
        "playlist-read-collaborative",
        "app-remote-control",
        "streaming",
    )
}
