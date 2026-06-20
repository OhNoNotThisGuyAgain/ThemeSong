package com.spotzones.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * [LocationProvider] over the fused provider. Geofencing (handled separately) does the heavy lifting
 * for zone detection; this stream is only used for the live map and for confirming a transition,
 * so it requests [Priority.PRIORITY_BALANCED_POWER_ACCURACY] at a relaxed interval to protect battery.
 */
@Singleton
class FusedLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
    private val permissions: PermissionChecker,
) : LocationProvider {

    @SuppressLint("MissingPermission") // guarded by PermissionChecker
    override suspend fun lastKnownLocation(): Outcome<GeoCoordinate> {
        if (!permissions.hasAnyLocation()) return Outcome.Failure(DomainError.PermissionDenied)
        return try {
            val location = client.lastLocation.await()
                ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            if (location != null) {
                Outcome.Success(GeoCoordinate(location.latitude, location.longitude))
            } else {
                Outcome.Failure(DomainError.LocationUnavailable)
            }
        } catch (e: SecurityException) {
            Outcome.Failure(DomainError.PermissionDenied)
        } catch (e: Exception) {
            Outcome.Failure(DomainError.LocationUnavailable)
        }
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<GeoCoordinate> = callbackFlow {
        if (!permissions.hasAnyLocation()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(10f)
            .build()

        val callback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(GeoCoordinate(it.latitude, it.longitude)) }
            }
        }
        client.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 15_000L
        const val FASTEST_INTERVAL_MS = 5_000L
    }
}
