package com.spotzones.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the automation engine should do when a rule wins. Actions are intentionally declarative;
 * translating them into Spotify App Remote calls is the playback layer's job.
 */
@Serializable
sealed interface Action {

    @Serializable
    @SerialName("play")
    data class Play(val config: PlaybackConfig) : Action

    @Serializable
    @SerialName("pause")
    data object Pause : Action

    @Serializable
    @SerialName("set_volume")
    data class SetVolume(val volumePercent: Int) : Action

    /** Do nothing — explicitly used by rules that should suppress lower-priority automation. */
    @Serializable
    @SerialName("none")
    data object None : Action
}
