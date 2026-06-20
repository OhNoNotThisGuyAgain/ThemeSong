package com.spotzones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.Schedule
import com.spotzones.domain.model.Zone

/**
 * Room row for a [Zone].
 *
 * Spatial/priority fields are real columns so the geofence sync and queries can filter cheaply,
 * while the playback config, schedule and advanced-trigger tree ride along as JSON (see [com.spotzones.data.local.Converters]).
 */
@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val priority: Int,
    val enabled: Boolean,
    val playback: PlaybackConfig,
    val schedule: Schedule,
    val advancedTrigger: Condition,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ZoneEntity.toDomain(): Zone = Zone(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    center = GeoCoordinate(latitude, longitude),
    radiusMeters = radiusMeters,
    priority = priority,
    enabled = enabled,
    playback = playback,
    schedule = schedule,
    advancedTrigger = advancedTrigger,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Zone.toEntity(): ZoneEntity = ZoneEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    latitude = center.latitude,
    longitude = center.longitude,
    radiusMeters = radiusMeters,
    priority = priority,
    enabled = enabled,
    playback = playback,
    schedule = schedule,
    advancedTrigger = advancedTrigger,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
