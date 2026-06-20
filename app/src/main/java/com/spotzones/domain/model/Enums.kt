package com.spotzones.domain.model

import kotlinx.serialization.Serializable

/** How Spotify should repeat within the zone's playlist. Mirrors the App Remote repeat modes. */
@Serializable
enum class RepeatMode { OFF, TRACK, CONTEXT }

/**
 * How playback should change when a zone becomes active.
 *
 * - [IMMEDIATE]: switch playlists at once (may cut the current song).
 * - [FINISH_SONG]: let the current track finish, then switch.
 * - [CROSSFADE]: blend over [PlaybackConfig.crossfadeSeconds] using a volume ramp.
 */
@Serializable
enum class TransitionMode { IMMEDIATE, FINISH_SONG, CROSSFADE }

/** Coarse movement classification derived from the Activity Recognition API. */
@Serializable
enum class MovementState { UNKNOWN, STATIONARY, WALKING, RUNNING, CYCLING, DRIVING }

/** Comparison operator for numeric conditions (battery level, speed). */
@Serializable
enum class Comparator { LESS_THAN, LESS_OR_EQUAL, EQUAL, GREATER_OR_EQUAL, GREATER_THAN;

    fun evaluate(lhs: Double, rhs: Double): Boolean = when (this) {
        LESS_THAN -> lhs < rhs
        LESS_OR_EQUAL -> lhs <= rhs
        EQUAL -> lhs == rhs
        GREATER_OR_EQUAL -> lhs >= rhs
        GREATER_THAN -> lhs > rhs
    }
}

/** Kind of audio output currently routed, used by headphone/car conditions. */
@Serializable
enum class AudioRoute { SPEAKER, WIRED_HEADPHONES, BLUETOOTH_HEADPHONES, BLUETOOTH_CAR, BLUETOOTH_OTHER, UNKNOWN }

/** Day of week, ISO-8601 ordering (Monday = 1) to match [java.time.DayOfWeek]. */
@Serializable
enum class Weekday(val isoValue: Int) {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7);

    companion object {
        fun fromIso(value: Int): Weekday = entries.first { it.isoValue == value }
    }
}
