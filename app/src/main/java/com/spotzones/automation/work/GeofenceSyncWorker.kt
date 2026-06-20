package com.spotzones.automation.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spotzones.domain.location.GeofenceManager
import com.spotzones.domain.repository.ZoneRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Re-registers all geofences. The OS drops geofences on reboot/app-update, so this runs from the
 * boot receiver; it's also a convenient way to recover from a transient registration failure.
 */
@HiltWorker
class GeofenceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val zoneRepository: ZoneRepository,
    private val geofenceManager: GeofenceManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val zones = zoneRepository.getEnabledZones()
        geofenceManager.syncGeofences(zones)
        Result.success()
    } catch (e: Exception) {
        Timber.w(e, "Geofence resync failed")
        if (runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "geofence_sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<GeofenceSyncWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
