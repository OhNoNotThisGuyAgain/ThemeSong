package com.spotzones.data.remote

import com.spotzones.domain.spotify.SpotifyAuth
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches a valid bearer token to every Web API request, transparently refreshing it first.
 *
 * [SpotifyAuth] is injected via [Provider] to break the dependency cycle: the auth implementation
 * uses its own (interceptor-free) Retrofit instance for the token endpoint, while this interceptor
 * guards the data API. `runBlocking` is acceptable here because OkHttp interceptors run on a
 * background dispatcher thread, never the main thread.
 */
class AuthInterceptor @Inject constructor(
    private val auth: Provider<SpotifyAuth>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { auth.get().validAccessToken().getOrNull() }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}
