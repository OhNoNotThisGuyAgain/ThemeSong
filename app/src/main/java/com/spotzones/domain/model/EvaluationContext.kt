package com.spotzones.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Immutable snapshot of everything the rule engine can reason about at one instant. Built by the
 * automation layer from sensors/system state and handed to the engine. Keeping it a plain data
 * class makes the engine pure and trivially unit-testable.
 *
 * Fields are nullable when the corresponding signal is unavailable (permission denied, sensor off);
 * conditions that depend on a missing signal evaluate to `false` rather than throwing.
 */
data class EvaluationContext(
    val location: GeoCoordinate?,
    val locationAccuracyMeters: Float?,
    val time: LocalTime,
    val dayOfWeek: DayOfWeek,
    val speedMph: Double?,
    val movementState: MovementState,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val connectedBluetooth: Set<BluetoothDeviceInfo>,
    val wifiSsid: String?,
    val audioRoute: AudioRoute,
    val headphonesConnected: Boolean,
)

/** Minimal description of a connected Bluetooth device used by trigger matching. */
data class BluetoothDeviceInfo(
    val address: String,
    val name: String?,
    val isAudio: Boolean,
)
