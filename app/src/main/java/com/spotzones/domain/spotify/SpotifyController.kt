package com.spotzones.domain.spotify

import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.PlaybackState
import com.spotzones.domain.util.Outcome
import kotlinx.coroutines.flow.StateFlow

/**
 * Port for controlling Spotify playback via the App Remote SDK.
 *
 * The domain layer depends only on this interface; the concrete App Remote implementation lives in
 * the data layer. [playbackState] is hot and bidirectional — changes made inside Spotify (or another
 * Connect device) flow back here, satisfying the "stay synchronized with Spotify" requirement.
 */
interface SpotifyController {

    val playbackState: StateFlow<PlaybackState>

    /** Establishes (or returns an existing) App Remote connection. Safe to call repeatedly. */
    suspend fun connect(): Outcome<Unit>

    fun disconnect()

    /**
     * Applies a full [PlaybackConfig]: optionally resumes the saved position, then sets shuffle,
     * repeat, volume and (for [com.spotzones.domain.model.TransitionMode.CROSSFADE]) ramps volume.
     */
    suspend fun apply(config: PlaybackConfig): Outcome<Unit>

    suspend fun resume(): Outcome<Unit>
    suspend fun pause(): Outcome<Unit>
    suspend fun skipNext(): Outcome<Unit>
    suspend fun skipPrevious(): Outcome<Unit>
    suspend fun seekTo(positionMs: Long): Outcome<Unit>
    suspend fun setShuffle(enabled: Boolean): Outcome<Unit>
    suspend fun setRepeat(mode: com.spotzones.domain.model.RepeatMode): Outcome<Unit>
    suspend fun togglePlayPause(): Outcome<Unit>
}
