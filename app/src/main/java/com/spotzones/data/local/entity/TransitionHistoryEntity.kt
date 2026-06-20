package com.spotzones.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spotzones.domain.model.TransitionHistory
import com.spotzones.domain.model.TransitionTrigger

@Entity(
    tableName = "transition_history",
    indices = [Index("enteredAt"), Index("zoneId")],
)
data class TransitionHistoryEntity(
    @PrimaryKey val id: String,
    val zoneId: String?,
    val zoneName: String,
    val playlistName: String?,
    val playlistUri: String?,
    val trigger: TransitionTrigger,
    val enteredAt: Long,
    val exitedAt: Long?,
    val skippedSongs: Int,
    val wasManualOverride: Boolean,
    val succeeded: Boolean,
    val failureReason: String?,
)

fun TransitionHistoryEntity.toDomain() = TransitionHistory(
    id, zoneId, zoneName, playlistName, playlistUri, trigger,
    enteredAt, exitedAt, skippedSongs, wasManualOverride, succeeded, failureReason,
)

fun TransitionHistory.toEntity() = TransitionHistoryEntity(
    id, zoneId, zoneName, playlistName, playlistUri, trigger,
    enteredAt, exitedAt, skippedSongs, wasManualOverride, succeeded, failureReason,
)
