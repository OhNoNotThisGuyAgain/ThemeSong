package com.spotzones.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.spotzones.data.local.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones ORDER BY priority DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE id = :id")
    fun observeById(id: String): Flow<ZoneEntity?>

    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getById(id: String): ZoneEntity?

    @Query("SELECT * FROM zones ORDER BY priority DESC")
    suspend fun getAll(): List<ZoneEntity>

    @Query("SELECT * FROM zones WHERE enabled = 1")
    suspend fun getEnabled(): List<ZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: ZoneEntity)

    @Query("UPDATE zones SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM zones WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM zones")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(zones: List<ZoneEntity>) {
        clear()
        zones.forEach { upsert(it) }
    }
}
