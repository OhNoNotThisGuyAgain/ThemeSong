package com.spotzones.domain.engine

import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.Zone

/**
 * Pure evaluator for the [Condition] tree against an [EvaluationContext].
 *
 * Design notes:
 * - Pure & side-effect-free → exhaustively unit-testable on the JVM with no Android dependencies.
 * - A missing signal (null in the context) makes its leaf condition `false` rather than throwing,
 *   so denied permissions degrade gracefully instead of breaking automation.
 * - [Condition.InsideZone] is resolved through a [zoneLookup] map so the same evaluator serves both
 *   the live engine and tests.
 */
class ConditionEvaluator {

    fun evaluate(
        condition: Condition,
        context: EvaluationContext,
        zoneLookup: (String) -> Zone?,
    ): Boolean = when (condition) {
        Condition.Always -> true
        is Condition.All -> condition.conditions.all { evaluate(it, context, zoneLookup) }
        is Condition.Any -> condition.conditions.isNotEmpty() &&
            condition.conditions.any { evaluate(it, context, zoneLookup) }
        is Condition.Not -> !evaluate(condition.condition, context, zoneLookup)

        is Condition.InsideZone -> {
            val zone = zoneLookup(condition.zoneId)
            val loc = context.location
            zone != null && loc != null && zone.contains(loc)
        }

        is Condition.TimeWindow -> {
            val minute = context.time.hour * 60 + context.time.minute
            val start = condition.startMinuteOfDay
            val end = condition.endMinuteOfDay
            when {
                start == end -> true // all-day
                end <= start -> minute >= start || minute < end // crosses midnight
                else -> minute in start until end
            }
        }

        is Condition.DayOfWeekIn -> com.spotzones.domain.model.Weekday
            .fromIso(context.dayOfWeek.value) in condition.days

        is Condition.BluetoothConnected -> context.connectedBluetooth.any { device ->
            val addressMatch = condition.address?.let { it.equals(device.address, ignoreCase = true) } ?: true
            val nameMatch = condition.nameContains?.let { needle ->
                device.name?.contains(needle, ignoreCase = true) == true
            } ?: true
            // When neither filter is set, "any device connected" satisfies it.
            if (condition.address == null && condition.nameContains == null) true else addressMatch && nameMatch
        }

        is Condition.Charging -> context.isCharging == condition.charging

        is Condition.BatteryLevel -> {
            val level = context.batteryPercent
            level != null && condition.comparator.evaluate(level.toDouble(), condition.percent.toDouble())
        }

        is Condition.WifiConnected ->
            context.wifiSsid != null && context.wifiSsid.equals(condition.ssid, ignoreCase = true)

        is Condition.Moving -> context.movementState in condition.states

        is Condition.Speed -> {
            val speed = context.speedMph
            speed != null && condition.comparator.evaluate(speed, condition.mph)
        }

        is Condition.HeadphonesConnected -> context.headphonesConnected == condition.connected

        is Condition.AudioRouteIs -> context.audioRoute in condition.routes
    }
}
