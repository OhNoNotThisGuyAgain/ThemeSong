package com.spotzones.domain.repository

import com.spotzones.domain.model.TransitionHistory
import kotlinx.coroutines.flow.Flow

/** Append-only timeline of zone/rule transitions. */
interface HistoryRepository {
    fun observeRecent(limit: Int = 100): Flow<List<TransitionHistory>>
    fun search(query: String): Flow<List<TransitionHistory>>
    suspend fun record(entry: TransitionHistory)
    /** Marks the most recent open entry for [zoneId] as exited at [timestamp]. */
    suspend fun closeOpenEntry(zoneId: String?, timestamp: Long, skippedSongs: Int)
    suspend fun clearOlderThan(timestamp: Long)
    suspend fun clearAll()
}
