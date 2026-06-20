package com.spotzones.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.spotzones.BuildConfig
import com.spotzones.core.serialization.AppJson
import com.spotzones.data.remote.AuthInterceptor
import com.spotzones.data.remote.api.SpotifyApiService
import com.spotzones.data.remote.api.SpotifyAuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AuthClient
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApiClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val jsonMediaType = "application/json".toMediaType()

    @Provides @Singleton
    fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        // Never log Authorization headers, even in debug.
        redactHeader("Authorization")
    }

    /** Client for the token endpoint — no auth interceptor (it *obtains* the token). */
    @Provides @Singleton @AuthClient
    fun authClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(logging).build()

    /** Client for the data API — adds and refreshes the bearer token transparently. */
    @Provides @Singleton @ApiClient
    fun apiClient(logging: HttpLoggingInterceptor, authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

    @Provides @Singleton
    fun authApi(@AuthClient client: OkHttpClient): SpotifyAuthApiService =
        Retrofit.Builder()
            .baseUrl(SpotifyAuthApiService.BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory(jsonMediaType))
            .build()
            .create(SpotifyAuthApiService::class.java)

    @Provides @Singleton
    fun spotifyApi(@ApiClient client: OkHttpClient): SpotifyApiService =
        Retrofit.Builder()
            .baseUrl(SpotifyApiService.BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory(jsonMediaType))
            .build()
            .create(SpotifyApiService::class.java)
}
