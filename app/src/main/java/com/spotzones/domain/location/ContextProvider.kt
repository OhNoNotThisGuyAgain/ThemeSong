package com.spotzones.domain.location

import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.util.Outcome
import kotlinx.coroutines.flow.Flow

/** Supplies the current location on demand and as a low-power stream. */
interface LocationProvider {
    suspend fun lastKnownLocation(): Outcome<GeoCoordinate>
    /** Adaptive-interval updates; cadence is chosen by the implementation per battery settings. */
    fun locationUpdates(): Flow<GeoCoordinate>
}

/**
 * Assembles a full [EvaluationContext] from all available signals (location, time, sensors, system
 * state). Centralising this keeps the rule engine pure and makes the snapshot easy to mock in tests.
 */
interface ContextProvider {
    suspend fun currentContext(): EvaluationContext
}
