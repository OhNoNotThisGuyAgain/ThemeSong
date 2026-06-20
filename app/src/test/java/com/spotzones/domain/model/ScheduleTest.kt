package com.spotzones.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Test

class ScheduleTest {

    @Test fun `all day matches any time on allowed day`() {
        val schedule = Schedule(daysOfWeek = setOf(Weekday.MONDAY))
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(3, 0))).isTrue()
        assertThat(schedule.matches(DayOfWeek.TUESDAY, LocalTime.of(3, 0))).isFalse()
    }

    @Test fun `window within same day`() {
        val schedule = Schedule(startMinuteOfDay = 9 * 60, endMinuteOfDay = 17 * 60)
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(12, 0))).isTrue()
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(8, 59))).isFalse()
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(17, 0))).isFalse() // end exclusive
    }

    @Test fun `window crossing midnight`() {
        val schedule = Schedule(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60)
        assertThat(schedule.crossesMidnight).isTrue()
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(23, 30))).isTrue()
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(5, 0))).isTrue()
        assertThat(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(7, 0))).isFalse()
    }
}
