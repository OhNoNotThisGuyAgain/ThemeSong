package com.spotzones.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for starting playback of a context (playlist) at an optional position. */
@Serializable
data class PlayRequestDto(
    @SerialName("context_uri") val contextUri: String? = null,
    @SerialName("position_ms") val positionMs: Long? = null,
    val offset: OffsetDto? = null,
)

@Serializable
data class OffsetDto(val position: Int? = null, val uri: String? = null)

/** Empty JSON body for "resume" (PUT play with no parameters). */
@Serializable
class EmptyBodyDto

// --- Player state ---

@Serializable
data class DeviceDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("volume_percent") val volumePercent: Int? = null,
)

@Serializable
data class ContextDto(val uri: String? = null, val type: String? = null)

@Serializable
data class PlayerStateDto(
    val device: DeviceDto? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long = 0,
    @SerialName("shuffle_state") val shuffleState: Boolean = false,
    @SerialName("repeat_state") val repeatState: String = "off",
    val context: ContextDto? = null,
    val item: TrackItemDto? = null,
)

@Serializable
data class TrackItemDto(
    val uri: String,
    val name: String,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val artists: List<ArtistRefDto> = emptyList(),
    val album: AlbumDto? = null,
)
