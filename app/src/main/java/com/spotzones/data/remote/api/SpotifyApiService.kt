package com.spotzones.data.remote.api

import com.spotzones.data.remote.dto.PagingPlaylistsDto
import com.spotzones.data.remote.dto.PlaylistDto
import com.spotzones.data.remote.dto.SearchResponseDto
import com.spotzones.data.remote.dto.UserProfileDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Spotify Web API surface. Auth is added by an OkHttp interceptor, so no token params appear here. */
interface SpotifyApiService {

    @GET("v1/me")
    suspend fun currentUser(): UserProfileDto

    @GET("v1/me/playlists")
    suspend fun userPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PagingPlaylistsDto

    @GET("v1/playlists/{id}")
    suspend fun playlist(@Path("id") id: String): PlaylistDto

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int = 20,
    ): SearchResponseDto

    // --- Player (Connect) control. Requires Spotify Premium and an active device. ---

    @GET("v1/me/player")
    suspend fun playerState(): retrofit2.Response<com.spotzones.data.remote.dto.PlayerStateDto>

    @retrofit2.http.PUT("v1/me/player/play")
    suspend fun play(@retrofit2.http.Body body: com.spotzones.data.remote.dto.PlayRequestDto): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/play")
    suspend fun resume(@retrofit2.http.Body body: com.spotzones.data.remote.dto.EmptyBodyDto = com.spotzones.data.remote.dto.EmptyBodyDto()): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/pause")
    suspend fun pause(): retrofit2.Response<Unit>

    @retrofit2.http.POST("v1/me/player/next")
    suspend fun next(): retrofit2.Response<Unit>

    @retrofit2.http.POST("v1/me/player/previous")
    suspend fun previous(): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/seek")
    suspend fun seek(@Query("position_ms") positionMs: Long): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/shuffle")
    suspend fun shuffle(@Query("state") state: Boolean): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/repeat")
    suspend fun repeat(@Query("state") state: String): retrofit2.Response<Unit>

    @retrofit2.http.PUT("v1/me/player/volume")
    suspend fun volume(@Query("volume_percent") percent: Int): retrofit2.Response<Unit>

    companion object {
        const val BASE_URL = "https://api.spotify.com/"
    }
}

/** Separate host for token exchange/refresh. */
interface SpotifyAuthApiService {
    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("api/token")
    suspend fun exchangeToken(
        @retrofit2.http.Field("grant_type") grantType: String,
        @retrofit2.http.Field("code") code: String? = null,
        @retrofit2.http.Field("redirect_uri") redirectUri: String? = null,
        @retrofit2.http.Field("client_id") clientId: String,
        @retrofit2.http.Field("code_verifier") codeVerifier: String? = null,
        @retrofit2.http.Field("refresh_token") refreshToken: String? = null,
    ): com.spotzones.data.remote.dto.TokenResponseDto

    companion object {
        const val BASE_URL = "https://accounts.spotify.com/"
    }
}
