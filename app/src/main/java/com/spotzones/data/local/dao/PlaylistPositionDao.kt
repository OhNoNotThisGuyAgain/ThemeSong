package com.spotzones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotzones.data.local.entity.PlaylistPositionEntity

@Dao
interface PlaylistPositionDao {
    @Query("SELECT * FROM playlist_positions WHERE contextUri = :contextUri")
    suspend fun get(contextUri: String): PlaylistPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PlaylistPositionEntity)
}
