package com.spotzones.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal, intentionally lenient DTOs covering only the fields SpotZones consumes. */

@Serializable
data class ImageDto(val url: String, val width: Int? = null, val height: Int? = null)

@Serializable
data class OwnerDto(@SerialName("display_name") val displayName: String? = null)

@Serializable
data class TracksRefDto(val total: Int? = null)

@Serializable
data class PlaylistDto(
    val uri: String,
    val name: String,
    val images: List<ImageDto> = emptyList(),
    val owner: OwnerDto? = null,
    val tracks: TracksRefDto? = null,
)

@Serializable
data class PagingPlaylistsDto(
    val items: List<PlaylistDto> = emptyList(),
    val next: String? = null,
)

@Serializable
data class ArtistRefDto(val name: String)

@Serializable
data class AlbumDto(val name: String, val images: List<ImageDto> = emptyList())

@Serializable
data class TrackDto(
    val uri: String,
    val name: String,
    val artists: List<ArtistRefDto> = emptyList(),
    val album: AlbumDto? = null,
)

@Serializable
data class ArtistDto(
    val uri: String,
    val name: String,
    val images: List<ImageDto> = emptyList(),
)

@Serializable
data class TrackSearchPageDto(val items: List<TrackDto> = emptyList())

@Serializable
data class ArtistSearchPageDto(val items: List<ArtistDto> = emptyList())

@Serializable
data class SearchResponseDto(
    val playlists: PagingPlaylistsDto? = null,
    val tracks: TrackSearchPageDto? = null,
    val artists: ArtistSearchPageDto? = null,
)

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)
