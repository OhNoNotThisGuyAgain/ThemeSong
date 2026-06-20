package com.spotzones.domain.ai

import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.PlaylistRef
import com.spotzones.domain.model.Schedule
import com.spotzones.domain.model.Zone

/**
 * Forward-looking interfaces for AI/ML features. These are deliberately *not* implemented yet — the
 * abstractions exist so the engine and UI can be wired to them later without churn. The default
 * Hilt bindings resolve to no-op/heuristic implementations that keep behaviour fully deterministic.
 */

/** Ranks playlists likely to be wanted in a given context (location/time/habits). */
interface PlaylistPredictor {
    suspend fun predict(context: EvaluationContext, candidates: List<PlaylistRef>): List<PlaylistRef>
}

/** Suggests a likely next zone for the dashboard's "next likely zone" surface. */
interface NextZonePredictor {
    suspend fun predictNext(current: Zone?, recent: List<Zone>): Zone?
}

/** Learns recurring schedules from observed dwell patterns. */
interface ScheduleLearner {
    suspend fun suggestSchedule(zoneId: String): Schedule?
}

/** Recommends songs/playlists based on location-specific listening history. */
interface ContextRecommender {
    suspend fun recommendForLocation(context: EvaluationContext): List<PlaylistRef>
}

/** No-op predictor used until real models ship; returns inputs unchanged / nulls. */
class HeuristicPlaylistPredictor : PlaylistPredictor {
    override suspend fun predict(context: EvaluationContext, candidates: List<PlaylistRef>) = candidates
}

class NoOpNextZonePredictor : NextZonePredictor {
    override suspend fun predictNext(current: Zone?, recent: List<Zone>): Zone? = recent.firstOrNull { it.id != current?.id }
}

class NoOpScheduleLearner : ScheduleLearner {
    override suspend fun suggestSchedule(zoneId: String): Schedule? = null
}

class NoOpContextRecommender : ContextRecommender {
    override suspend fun recommendForLocation(context: EvaluationContext): List<PlaylistRef> = emptyList()
}
