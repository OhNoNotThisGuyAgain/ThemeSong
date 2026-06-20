package com.spotzones.domain.model

import kotlinx.serialization.Serializable

/** Reference to a Spotify playable context (playlist, album or artist). */
@Serializable
data class PlaylistRef(
    val uri: String,
    val name: String,
    val imageUrl: String? = null,
    val ownerName: String? = null,
    val trackCount: Int? = null,
) {
    val isValid: Boolean get() = uri.startsWith("spotify:")
}

/**
 * Everything SpotZones tells Spotify to do when a zone activates. Volume is 0..100; crossfade is
 * clamped to Spotify's supported 0..12s range by the playback layer.
 */
@Serializable
data class PlaybackConfig(
    val playlist: PlaylistRef?,
    val shuffle: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.CONTEXT,
    val transitionMode: TransitionMode = TransitionMode.CROSSFADE,
    val crossfadeSeconds: Int = 6,
    val volumePercent: Int = 80,
    val fadeInVolume: Boolean = true,
    val fadeOutVolume: Boolean = false,
    /** Resume the saved position for this playlist instead of restarting it. */
    val resumePlaybackPosition: Boolean = true,
) {
    init {
        require(volumePercent in 0..100) { "volume out of range" }
        require(crossfadeSeconds in 0..12) { "crossfade out of Spotify range" }
    }
}
