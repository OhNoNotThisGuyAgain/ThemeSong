package com.spotzones.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.serialization.Serializable

/**
 * Time window during which a zone is allowed to be active.
 *
 * [startMinuteOfDay]/[endMinuteOfDay] are minutes since midnight (0..1439). A window where end is
 * less than or equal to start is treated as crossing midnight (e.g. 22:00–06:00), which is the
 * common "night" case and must be handled explicitly.
 */
@Serializable
data class Schedule(
    val daysOfWeek: Set<Weekday> = Weekday.entries.toSet(),
    val startMinuteOfDay: Int = 0,
    val endMinuteOfDay: Int = 0,
) {
    val isAllDay: Boolean get() = startMinuteOfDay == endMinuteOfDay
    val crossesMidnight: Boolean get() = !isAllDay && endMinuteOfDay <= startMinuteOfDay

    fun matches(day: DayOfWeek, time: LocalTime): Boolean {
        if (Weekday.fromIso(day.value) !in daysOfWeek) return false
        if (isAllDay) return true
        val minute = time.hour * 60 + time.minute
        return if (crossesMidnight) {
            minute >= startMinuteOfDay || minute < endMinuteOfDay
        } else {
            minute in startMinuteOfDay until endMinuteOfDay
        }
    }

    companion object {
        val ALWAYS = Schedule()
    }
}
