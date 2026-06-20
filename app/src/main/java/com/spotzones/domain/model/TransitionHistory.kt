package com.spotzones.domain.model

import java.util.UUID

/** Why a transition happened, useful for the history screen and future habit learning. */
enum class TransitionTrigger { ZONE_ENTER, ZONE_EXIT, RULE_MATCH, MANUAL, SCHEDULE, PRIORITY_OVERRIDE }

/**
 * One row in the transition timeline: SpotZones changed (or attempted to change) playback because a
 * zone/rule activated. [exitedAt] is null while the zone is still active.
 */
data class TransitionHistory(
    val id: String = UUID.randomUUID().toString(),
    val zoneId: String?,
    val zoneName: String,
    val playlistName: String?,
    val playlistUri: String?,
    val trigger: TransitionTrigger,
    val enteredAt: Long,
    val exitedAt: Long?,
    val skippedSongs: Int = 0,
    val wasManualOverride: Boolean = false,
    val succeeded: Boolean = true,
    val failureReason: String? = null,
) {
    val durationMs: Long? get() = exitedAt?.let { it - enteredAt }
}
