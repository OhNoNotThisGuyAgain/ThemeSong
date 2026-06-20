package com.spotzones.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A composable, serializable predicate over an [EvaluationContext].
 *
 * Conditions form a tree so users can express arbitrary AND/OR/NOT logic, e.g.
 * `All(InsideZone, TimeWindow(17:00–20:00), BluetoothConnected("AirPods"))`.
 *
 * Evaluation lives in the engine (see `RuleEvaluator`), not here, so the model stays a pure data
 * description that can be persisted and round-tripped through JSON for import/export.
 */
@Serializable
sealed interface Condition {

    /** Always matches — the default for a zone with no advanced triggers. */
    @Serializable
    @SerialName("always")
    data object Always : Condition

    /** Logical AND. Empty list matches (vacuous truth). */
    @Serializable
    @SerialName("all")
    data class All(val conditions: List<Condition>) : Condition

    /** Logical OR. Empty list does not match. */
    @Serializable
    @SerialName("any")
    data class Any(val conditions: List<Condition>) : Condition

    /** Logical NOT. */
    @Serializable
    @SerialName("not")
    data class Not(val condition: Condition) : Condition

    /** True when the current location is within [zoneId]'s radius. Resolved by the engine. */
    @Serializable
    @SerialName("inside_zone")
    data class InsideZone(val zoneId: String) : Condition

    /** Minutes-of-day window; crosses midnight when end <= start. */
    @Serializable
    @SerialName("time_window")
    data class TimeWindow(val startMinuteOfDay: Int, val endMinuteOfDay: Int) : Condition

    @Serializable
    @SerialName("day_of_week")
    data class DayOfWeekIn(val days: Set<Weekday>) : Condition

    @Serializable
    @SerialName("bluetooth")
    data class BluetoothConnected(val address: String? = null, val nameContains: String? = null) : Condition

    @Serializable
    @SerialName("charging")
    data class Charging(val charging: Boolean) : Condition

    @Serializable
    @SerialName("battery")
    data class BatteryLevel(val comparator: Comparator, val percent: Int) : Condition

    @Serializable
    @SerialName("wifi")
    data class WifiConnected(val ssid: String) : Condition

    @Serializable
    @SerialName("movement")
    data class Moving(val states: Set<MovementState>) : Condition

    @Serializable
    @SerialName("speed")
    data class Speed(val comparator: Comparator, val mph: Double) : Condition

    @Serializable
    @SerialName("headphones")
    data class HeadphonesConnected(val connected: Boolean = true) : Condition

    @Serializable
    @SerialName("audio_route")
    data class AudioRouteIs(val routes: Set<AudioRoute>) : Condition
}
