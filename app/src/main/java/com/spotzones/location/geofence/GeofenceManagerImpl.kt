package com.spotzones.location.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.location.GeofenceManager
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.Zone
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Registers zones with the platform Geofencing API.
 *
 * Battery strategy: the OS — not the app — monitors these geofences and only wakes SpotZones on a
 * transition. The API caps active geofences at 100 per app, so when there are more zones we register
 * the nearest [MAX_GEOFENCES] to the user's current location (a zone you're far from can't trigger
 * before you approach it, and the set is re-synced on every significant move and on app open).
 */
@Singleton
class GeofenceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient,
    private val locationProvider: LocationProvider,
    private val permissions: PermissionChecker,
) : GeofenceManager {

    @SuppressLint("MissingPermission") // guarded below
    override suspend fun syncGeofences(zones: List<Zone>): Outcome<Unit> {
        if (!permissions.hasFineLocation()) return Outcome.Failure(DomainError.PermissionDenied)

        return try {
            // Clear the previous set first so removed/edited zones never linger in the OS.
            geofencingClient.removeGeofences(pendingIntent).await()

            val selected = selectZones(zones)
            if (selected.isEmpty()) return Outcome.Success(Unit)

            val geofences = selected.map { it.toGeofence() }
            val request = GeofencingRequest.Builder()
                // Fire ENTER immediately if the device is already inside a freshly added geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            geofencingClient.addGeofences(request, pendingIntent).await()
            Outcome.Success(Unit)
        } catch (e: SecurityException) {
            Outcome.Failure(DomainError.PermissionDenied)
        } catch (e: Exception) {
            Outcome.Failure(DomainError.Unexpected(e.message ?: "Geofence sync failed", e))
        }
    }

    override suspend fun removeAll(): Outcome<Unit> = try {
        geofencingClient.removeGeofences(pendingIntent).await()
        Outcome.Success(Unit)
    } catch (e: Exception) {
        Outcome.Failure(DomainError.Unexpected(e.message ?: "Geofence removal failed", e))
    }

    private suspend fun selectZones(zones: List<Zone>): List<Zone> {
        if (zones.size <= MAX_GEOFENCES) return zones
        val here = locationProvider.lastKnownLocation().getOrNull()
            ?: return zones.sortedByDescending { it.priority }.take(MAX_GEOFENCES)
        return zones.sortedBy { it.center.distanceTo(here) }.take(MAX_GEOFENCES)
    }

    private fun Zone.toGeofence(): Geofence = Geofence.Builder()
        .setRequestId(id)
        .setCircularRegion(center.latitude, center.longitude, radiusMeters.toFloat())
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        // A relaxed responsiveness lets the OS batch transitions to save battery; the dwell debounce
        // against noisy GPS at zone edges is handled by the coordinator's idempotent diffing.
        .setNotificationResponsiveness(RESPONSIVENESS_MS)
        .build()

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private companion object {
        const val MAX_GEOFENCES = 95 // headroom under the 100 platform limit
        const val RESPONSIVENESS_MS = 15_000
    }
}
