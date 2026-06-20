package com.spotzones.data.repository

import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.local.dao.ZoneDao
import com.spotzones.data.local.entity.toDomain
import com.spotzones.data.local.entity.toEntity
import com.spotzones.domain.location.GeofenceManager
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import com.spotzones.domain.util.onFailure
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed [ZoneRepository]. Every mutation re-syncs the platform geofences so the OS always
 * watches the current set — keeping persistence and geofencing atomic from the caller's view.
 */
@Singleton
class ZoneRepositoryImpl @Inject constructor(
    private val zoneDao: ZoneDao,
    private val geofenceManager: GeofenceManager,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ZoneRepository {

    override fun observeZones(): Flow<List<Zone>> =
        zoneDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeZone(id: String): Flow<Zone?> =
        zoneDao.observeById(id).map { it?.toDomain() }

    override suspend fun getZone(id: String): Zone? =
        withContext(io) { zoneDao.getById(id)?.toDomain() }

    override suspend fun getAllZones(): List<Zone> =
        withContext(io) { zoneDao.getAll().map { it.toDomain() } }

    override suspend fun getEnabledZones(): List<Zone> =
        withContext(io) { zoneDao.getEnabled().map { it.toDomain() } }

    override suspend fun upsert(zone: Zone): Outcome<Zone> = withContext(io) {
        Outcome.catching {
            val normalized = zone.copy(updatedAt = System.currentTimeMillis())
            zoneDao.upsert(normalized.toEntity())
            resyncGeofences()
            normalized
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> = withContext(io) {
        Outcome.catching {
            zoneDao.delete(id)
            resyncGeofences()
        }
    }

    override suspend fun setEnabled(id: String, enabled: Boolean): Outcome<Unit> = withContext(io) {
        Outcome.catching {
            zoneDao.setEnabled(id, enabled, System.currentTimeMillis())
            resyncGeofences()
        }
    }

    override suspend fun replaceAll(zones: List<Zone>): Outcome<Unit> = withContext(io) {
        Outcome.catching {
            zoneDao.replaceAll(zones.map { it.toEntity() })
            resyncGeofences()
        }
    }

    private suspend fun resyncGeofences() {
        val enabled = zoneDao.getEnabled().map { it.toDomain() }
        // Geofence failures must not roll back persistence; surface as a soft failure via logs.
        geofenceManager.syncGeofences(enabled).onFailure {
            if (it is DomainError.PermissionDenied) {
                timber.log.Timber.w("Geofence sync skipped: location permission missing")
            } else {
                timber.log.Timber.w("Geofence sync failed: %s", it.message)
            }
        }
    }
}
