package com.spotzones.data.repository

import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.local.dao.PlaylistPositionDao
import com.spotzones.data.local.entity.toDomain
import com.spotzones.data.local.entity.toEntity
import com.spotzones.domain.model.PlaylistPosition
import com.spotzones.domain.repository.PlaybackPositionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class PlaybackPositionRepositoryImpl @Inject constructor(
    private val dao: PlaylistPositionDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PlaybackPositionRepository {

    override suspend fun get(contextUri: String): PlaylistPosition? =
        withContext(io) { dao.get(contextUri)?.toDomain() }

    override suspend fun save(position: PlaylistPosition) =
        withContext(io) { dao.upsert(position.toEntity()) }
}
