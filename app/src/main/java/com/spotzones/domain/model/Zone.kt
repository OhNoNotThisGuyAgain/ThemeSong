package com.spotzones.domain.model

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * A geofenced place with associated playback behaviour — the central user-facing entity.
 *
 * A zone is location-anchored ([center] + [radiusMeters]) but its activation can be further gated
 * by a [schedule] and an arbitrary [advancedTrigger] condition tree (Bluetooth, charging, speed…).
 * When several zones are simultaneously satisfied, [priority] (1..100, higher wins) breaks the tie,
 * which is how a tight "Bedroom" zone overrides a large enclosing "Home" zone.
 */
@Serializable
data class Zone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String = "place",
    val colorHex: Long = 0xFF1DB954,
    val center: GeoCoordinate,
    val radiusMeters: Double,
    val priority: Int = 50,
    val enabled: Boolean = true,
    val playback: PlaybackConfig,
    val schedule: Schedule = Schedule.ALWAYS,
    val advancedTrigger: Condition = Condition.Always,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(name.isNotBlank()) { "zone name required" }
        // Down to 5 m so room-scale zones are possible. Note: GPS accuracy limits how reliably the
        // OS geofence fires below ~100 m; the in-app evaluator still uses exact distance, so small
        // zones work best with good signal / the foreground service running.
        require(radiusMeters in 5.0..50_000.0) { "radius out of supported range" }
        require(priority in 1..100) { "priority must be 1..100" }
    }

    /** Whether [point] lies within the geofence circle. */
    fun contains(point: GeoCoordinate): Boolean = center.distanceTo(point) <= radiusMeters

    /**
     * Full activation predicate combining location, schedule and advanced triggers into a single
     * tree, so the same evaluator handles zones and standalone rules uniformly.
     */
    fun activationCondition(): Condition = Condition.All(
        buildList {
            add(Condition.InsideZone(id))
            if (!schedule.isAllDay || schedule.daysOfWeek.size < 7) {
                add(Condition.DayOfWeekIn(schedule.daysOfWeek))
                if (!schedule.isAllDay) {
                    add(Condition.TimeWindow(schedule.startMinuteOfDay, schedule.endMinuteOfDay))
                }
            }
            if (advancedTrigger != Condition.Always) add(advancedTrigger)
        },
    )
}
