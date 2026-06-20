package com.spotzones.location

import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.MovementState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains a coarse movement classification and speed estimate.
 *
 * The estimate is derived from successive location fixes fed in by the automation service (so it
 * costs nothing extra beyond the location updates the app already collects). Thresholds map speed to
 * walking/driving etc., which is sufficient for the speed/movement triggers without the battery cost
 * of continuous Activity Recognition. The interface is kept small so a richer AR-backed
 * implementation can be swapped in via DI later.
 */
@Singleton
class MovementTracker @Inject constructor() {

    private data class Fix(val coordinate: GeoCoordinate, val timestampMs: Long)

    @Volatile private var lastFix: Fix? = null
    @Volatile private var speedMph: Double? = null
    @Volatile private var state: MovementState = MovementState.UNKNOWN

    /** Feed a new fix; recomputes speed and movement state. [speedMetersPerSecond] is used when the
     *  platform supplies it (more accurate than differencing positions). */
    fun onLocation(coordinate: GeoCoordinate, timestampMs: Long, speedMetersPerSecond: Float?) {
        val previous = lastFix
        val mps: Double? = when {
            speedMetersPerSecond != null && speedMetersPerSecond >= 0f -> speedMetersPerSecond.toDouble()
            previous != null -> {
                val dtSeconds = (timestampMs - previous.timestampMs) / 1000.0
                if (dtSeconds > 0.5) coordinate.distanceTo(previous.coordinate) / dtSeconds else null
            }
            else -> null
        }
        lastFix = Fix(coordinate, timestampMs)
        if (mps != null) {
            speedMph = mps * MPS_TO_MPH
            state = classify(mps)
        }
    }

    fun currentSpeedMph(): Double? = speedMph

    fun currentState(): MovementState = state

    private fun classify(mps: Double): MovementState = when {
        mps < 0.4 -> MovementState.STATIONARY
        mps < 2.2 -> MovementState.WALKING       // < ~5 mph
        mps < 4.0 -> MovementState.RUNNING        // < ~9 mph
        mps < 7.0 -> MovementState.CYCLING        // < ~16 mph
        else -> MovementState.DRIVING
    }

    private companion object {
        const val MPS_TO_MPH = 2.236936
    }
}
