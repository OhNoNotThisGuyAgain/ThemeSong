package com.spotzones.domain.repository

import com.spotzones.domain.model.Zone
import com.spotzones.domain.util.Outcome
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for [Zone]s. Implemented by the data layer over Room. */
interface ZoneRepository {
    fun observeZones(): Flow<List<Zone>>
    fun observeZone(id: String): Flow<Zone?>
    suspend fun getZone(id: String): Zone?
    suspend fun getAllZones(): List<Zone>
    suspend fun getEnabledZones(): List<Zone>
    suspend fun upsert(zone: Zone): Outcome<Zone>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun setEnabled(id: String, enabled: Boolean): Outcome<Unit>
    /** Bulk replace used by import/restore. */
    suspend fun replaceAll(zones: List<Zone>): Outcome<Unit>
}
