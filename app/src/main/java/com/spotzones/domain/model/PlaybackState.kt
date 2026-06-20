package com.spotzones.domain.model

/** Metadata for the currently loaded track. */
data class TrackInfo(
    val uri: String,
    val title: String,
    val artists: String,
    val album: String,
    val imageUri: String?,
    val durationMs: Long,
)

/**
 * Snapshot of Spotify's playback, kept in sync bidirectionally with the App Remote. The Now Playing
 * screen renders this directly; it is the single source of truth for "what is happening right now".
 */
data class PlaybackState(
    val connection: SpotifyConnectionState,
    val isPlaying: Boolean,
    val track: TrackInfo?,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: RepeatMode,
    val contextUri: String?,
    val contextName: String?,
    val deviceName: String?,
) {
    companion object {
        val Disconnected = PlaybackState(
            connection = SpotifyConnectionState.Disconnected,
            isPlaying = false,
            track = null,
            positionMs = 0,
            shuffle = false,
            repeatMode = RepeatMode.OFF,
            contextUri = null,
            contextName = null,
            deviceName = null,
        )
    }
}

/** Connection lifecycle for the Spotify App Remote, surfaced to the UI for honest status reporting. */
sealed interface SpotifyConnectionState {
    data object Disconnected : SpotifyConnectionState
    data object Connecting : SpotifyConnectionState
    data object Connected : SpotifyConnectionState
    /** Spotify app is not installed. */
    data object NotInstalled : SpotifyConnectionState
    data class Failed(val reason: String) : SpotifyConnectionState
}

/** Last-known playback position per playlist, enabling "resume where you left off" per zone. */
data class PlaylistPosition(
    val contextUri: String,
    val trackUri: String,
    val positionMs: Long,
    val updatedAt: Long,
)
