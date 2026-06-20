package com.spotzones.data.repository

import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.local.dao.HistoryDao
import com.spotzones.data.local.entity.toDomain
import com.spotzones.data.local.entity.toEntity
import com.spotzones.domain.model.TransitionHistory
import com.spotzones.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<TransitionHistory>> =
        historyDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<TransitionHistory>> =
        historyDao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun record(entry: TransitionHistory) =
        withContext(io) { historyDao.insert(entry.toEntity()) }

    override suspend fun closeOpenEntry(zoneId: String?, timestamp: Long, skippedSongs: Int) =
        withContext(io) {
            historyDao.findOpenEntry(zoneId)?.let { open ->
                historyDao.insert(open.copy(exitedAt = timestamp, skippedSongs = skippedSongs))
            }
            Unit
        }

    override suspend fun clearOlderThan(timestamp: Long) =
        withContext(io) { historyDao.clearOlderThan(timestamp) }

    override suspend fun clearAll() = withContext(io) { historyDao.clearAll() }
}
