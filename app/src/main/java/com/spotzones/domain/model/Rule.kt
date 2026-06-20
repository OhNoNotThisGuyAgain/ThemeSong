package com.spotzones.domain.model

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * A standalone IF/THEN automation that is not tied to a single geofence, e.g.
 * "IF speed > 50 mph THEN play Road Trip". Zones are converted into rules internally so the engine
 * has one uniform evaluation path; this type exists for rules users author directly.
 */
@Serializable
data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 50,
    val condition: Condition,
    val action: Action,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(name.isNotBlank()) { "rule name required" }
        require(priority in 1..100) { "priority must be 1..100" }
    }
}

/**
 * Unified candidate the engine ranks. Both [Zone]s and [Rule]s collapse into this so priority
 * resolution and "winner" selection have a single, well-tested code path.
 */
data class EvaluationCandidate(
    val id: String,
    val displayName: String,
    val priority: Int,
    val condition: Condition,
    val action: Action,
    val sourceZone: Zone?,
)
