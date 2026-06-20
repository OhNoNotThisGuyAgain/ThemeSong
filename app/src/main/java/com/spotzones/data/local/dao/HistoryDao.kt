package com.spotzones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotzones.data.local.entity.TransitionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM transition_history ORDER BY enteredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransitionHistoryEntity>>

    @Query(
        """
        SELECT * FROM transition_history
        WHERE zoneName LIKE '%' || :query || '%'
           OR playlistName LIKE '%' || :query || '%'
        ORDER BY enteredAt DESC LIMIT 200
        """,
    )
    fun search(query: String): Flow<List<TransitionHistoryEntity>>

    @Query("SELECT * FROM transition_history WHERE zoneId IS :zoneId AND exitedAt IS NULL ORDER BY enteredAt DESC LIMIT 1")
    suspend fun findOpenEntry(zoneId: String?): TransitionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TransitionHistoryEntity)

    @Query("DELETE FROM transition_history WHERE enteredAt < :timestamp")
    suspend fun clearOlderThan(timestamp: Long)

    @Query("DELETE FROM transition_history")
    suspend fun clearAll()
}
