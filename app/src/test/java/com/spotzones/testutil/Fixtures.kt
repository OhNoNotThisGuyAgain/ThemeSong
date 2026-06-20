package com.spotzones.testutil

import com.spotzones.domain.model.AudioRoute
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.MovementState
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.PlaylistRef
import com.spotzones.domain.model.Schedule
import com.spotzones.domain.model.Zone
import java.time.DayOfWeek
import java.time.LocalTime

/** Shared builders so tests read as intent, not boilerplate. */
object Fixtures {

    fun playlist(name: String = "Test", uri: String = "spotify:playlist:$name") =
        PlaylistRef(uri = uri, name = name)

    fun playback(name: String = "Test") = PlaybackConfig(playlist = playlist(name))

    fun zone(
        id: String,
        name: String = id,
        center: GeoCoordinate = GeoCoordinate(40.0, -73.0),
        radiusMeters: Double = 200.0,
        priority: Int = 50,
        enabled: Boolean = true,
        schedule: Schedule = Schedule.ALWAYS,
        advancedTrigger: Condition = Condition.Always,
        updatedAt: Long = 0L,
    ) = Zone(
        id = id,
        name = name,
        center = center,
        radiusMeters = radiusMeters,
        priority = priority,
        enabled = enabled,
        playback = playback(name),
        schedule = schedule,
        advancedTrigger = advancedTrigger,
        updatedAt = updatedAt,
    )

    fun context(
        location: GeoCoordinate? = GeoCoordinate(40.0, -73.0),
        time: LocalTime = LocalTime.of(12, 0),
        day: DayOfWeek = DayOfWeek.WEDNESDAY,
        speedMph: Double? = 0.0,
        movement: MovementState = MovementState.STATIONARY,
        battery: Int? = 80,
        charging: Boolean = false,
        bluetooth: Set<com.spotzones.domain.model.BluetoothDeviceInfo> = emptySet(),
        wifi: String? = null,
        audioRoute: AudioRoute = AudioRoute.SPEAKER,
        headphones: Boolean = false,
    ) = EvaluationContext(
        location = location,
        locationAccuracyMeters = 10f,
        time = time,
        dayOfWeek = day,
        speedMph = speedMph,
        movementState = movement,
        batteryPercent = battery,
        isCharging = charging,
        connectedBluetooth = bluetooth,
        wifiSsid = wifi,
        audioRoute = audioRoute,
        headphonesConnected = headphones,
    )
}
