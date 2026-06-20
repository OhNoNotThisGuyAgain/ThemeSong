package com.spotzones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spotzones.domain.model.PlaylistPosition

@Entity(tableName = "playlist_positions")
data class PlaylistPositionEntity(
    @PrimaryKey val contextUri: String,
    val trackUri: String,
    val positionMs: Long,
    val updatedAt: Long,
)

fun PlaylistPositionEntity.toDomain() = PlaylistPosition(contextUri, trackUri, positionMs, updatedAt)

fun PlaylistPosition.toEntity() = PlaylistPositionEntity(contextUri, trackUri, positionMs, updatedAt)
