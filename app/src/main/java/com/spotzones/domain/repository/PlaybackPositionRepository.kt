package com.spotzones.domain.repository

import com.spotzones.domain.model.PlaylistPosition

/** Remembers per-playlist resume points so returning to a zone continues where you left off. */
interface PlaybackPositionRepository {
    suspend fun get(contextUri: String): PlaylistPosition?
    suspend fun save(position: PlaylistPosition)
}
