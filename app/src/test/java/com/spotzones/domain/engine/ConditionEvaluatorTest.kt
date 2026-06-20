package com.spotzones.domain.engine

import com.google.common.truth.Truth.assertThat
import com.spotzones.domain.model.AudioRoute
import com.spotzones.domain.model.BluetoothDeviceInfo
import com.spotzones.domain.model.Comparator
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.MovementState
import com.spotzones.domain.model.Weekday
import com.spotzones.domain.model.Zone
import com.spotzones.testutil.Fixtures
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Test

class ConditionEvaluatorTest {

    private val evaluator = ConditionEvaluator()
    private val home = Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 300.0)
    private val lookup: (String) -> Zone? = mapOf(home.id to home)::get

    private fun eval(condition: Condition, context: com.spotzones.domain.model.EvaluationContext) =
        evaluator.evaluate(condition, context, lookup)

    @Test fun `always is always true`() {
        assertThat(eval(Condition.Always, Fixtures.context())).isTrue()
    }

    @Test fun `inside zone true when within radius`() {
        val ctx = Fixtures.context(location = GeoCoordinate(40.0005, -73.0))
        assertThat(eval(Condition.InsideZone("home"), ctx)).isTrue()
    }

    @Test fun `inside zone false when outside radius`() {
        val ctx = Fixtures.context(location = GeoCoordinate(41.0, -73.0))
        assertThat(eval(Condition.InsideZone("home"), ctx)).isFalse()
    }

    @Test fun `inside zone false when location unknown`() {
        val ctx = Fixtures.context(location = null)
        assertThat(eval(Condition.InsideZone("home"), ctx)).isFalse()
    }

    @Test fun `time window within range`() {
        val ctx = Fixtures.context(time = LocalTime.of(18, 30))
        assertThat(eval(Condition.TimeWindow(17 * 60, 20 * 60), ctx)).isTrue()
    }

    @Test fun `time window crossing midnight`() {
        val night = Condition.TimeWindow(22 * 60, 6 * 60)
        assertThat(eval(night, Fixtures.context(time = LocalTime.of(23, 0)))).isTrue()
        assertThat(eval(night, Fixtures.context(time = LocalTime.of(5, 0)))).isTrue()
        assertThat(eval(night, Fixtures.context(time = LocalTime.of(12, 0)))).isFalse()
    }

    @Test fun `day of week matches`() {
        val weekdays = Condition.DayOfWeekIn(setOf(Weekday.MONDAY, Weekday.FRIDAY))
        assertThat(eval(weekdays, Fixtures.context(day = DayOfWeek.FRIDAY))).isTrue()
        assertThat(eval(weekdays, Fixtures.context(day = DayOfWeek.SUNDAY))).isFalse()
    }

    @Test fun `battery comparator`() {
        val low = Condition.BatteryLevel(Comparator.LESS_THAN, 20)
        assertThat(eval(low, Fixtures.context(battery = 10))).isTrue()
        assertThat(eval(low, Fixtures.context(battery = 50))).isFalse()
        assertThat(eval(low, Fixtures.context(battery = null))).isFalse()
    }

    @Test fun `speed comparator missing signal is false`() {
        val fast = Condition.Speed(Comparator.GREATER_THAN, 50.0)
        assertThat(eval(fast, Fixtures.context(speedMph = 70.0))).isTrue()
        assertThat(eval(fast, Fixtures.context(speedMph = null))).isFalse()
    }

    @Test fun `bluetooth name match`() {
        val airpods = Condition.BluetoothConnected(nameContains = "AirPods")
        val connected = Fixtures.context(
            bluetooth = setOf(BluetoothDeviceInfo("00:11", "Brendon's AirPods", isAudio = true)),
        )
        assertThat(eval(airpods, connected)).isTrue()
        assertThat(eval(airpods, Fixtures.context())).isFalse()
    }

    @Test fun `movement and audio route`() {
        assertThat(
            eval(Condition.Moving(setOf(MovementState.DRIVING)), Fixtures.context(movement = MovementState.DRIVING)),
        ).isTrue()
        assertThat(
            eval(Condition.AudioRouteIs(setOf(AudioRoute.BLUETOOTH_CAR)), Fixtures.context(audioRoute = AudioRoute.BLUETOOTH_CAR)),
        ).isTrue()
    }

    @Test fun `boolean composition all any not`() {
        val ctx = Fixtures.context(time = LocalTime.of(18, 0), charging = true)
        val condition = Condition.All(
            listOf(
                Condition.TimeWindow(17 * 60, 20 * 60),
                Condition.Any(listOf(Condition.Charging(true), Condition.Charging(false))),
                Condition.Not(Condition.Moving(setOf(MovementState.DRIVING))),
            ),
        )
        assertThat(eval(condition, ctx)).isTrue()
    }

    @Test fun `empty any is false, empty all is true`() {
        assertThat(eval(Condition.Any(emptyList()), Fixtures.context())).isFalse()
        assertThat(eval(Condition.All(emptyList()), Fixtures.context())).isTrue()
    }
}
