package com.spotzones.domain.location

import com.spotzones.domain.model.Zone
import com.spotzones.domain.util.Outcome

/**
 * Registers zones with the platform Geofencing API. The OS, not the app, watches these geofences,
 * which is the key to low battery usage — SpotZones is only woken on enter/exit transitions.
 *
 * The Geofencing API caps the number of active geofences (100 per app), so implementations
 * register the highest-priority / nearest subset; see the data-layer implementation for the policy.
 */
interface GeofenceManager {
    suspend fun syncGeofences(zones: List<Zone>): Outcome<Unit>
    suspend fun removeAll(): Outcome<Unit>
}
