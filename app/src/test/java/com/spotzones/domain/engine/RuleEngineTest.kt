package com.spotzones.domain.engine

import com.google.common.truth.Truth.assertThat
import com.spotzones.domain.model.Action
import com.spotzones.domain.model.Comparator
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.Rule
import com.spotzones.domain.model.Schedule
import com.spotzones.domain.model.Weekday
import com.spotzones.testutil.Fixtures
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Test

class RuleEngineTest {

    private val engine = RuleEngine()

    @Test fun `no zones yields no winner`() {
        val decision = engine.evaluate(emptyList(), emptyList(), Fixtures.context())
        assertThat(decision.winner).isNull()
        assertThat(decision.action).isEqualTo(Action.None)
    }

    @Test fun `single matching zone wins`() {
        val home = Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 500.0)
        val decision = engine.evaluate(listOf(home), emptyList(), Fixtures.context(location = GeoCoordinate(40.0, -73.0)))
        assertThat(decision.winner?.id).isEqualTo("home")
    }

    @Test fun `smaller radius overrides larger when same priority - bedroom beats home`() {
        val home = Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 500.0, priority = 50)
        val bedroom = Fixtures.zone("bedroom", center = GeoCoordinate(40.0, -73.0), radiusMeters = 20.0, priority = 50)
        val decision = engine.evaluate(
            listOf(home, bedroom),
            emptyList(),
            Fixtures.context(location = GeoCoordinate(40.0, -73.0)),
        )
        assertThat(decision.winner?.id).isEqualTo("bedroom")
        assertThat(decision.matches.map { it.id }).containsExactly("bedroom", "home").inOrder()
    }

    @Test fun `higher priority always wins regardless of radius`() {
        val tightLowPriority = Fixtures.zone("tight", radiusMeters = 20.0, priority = 10)
        val wideHighPriority = Fixtures.zone("wide", radiusMeters = 500.0, priority = 90)
        val decision = engine.evaluate(listOf(tightLowPriority, wideHighPriority), emptyList(), Fixtures.context())
        assertThat(decision.winner?.id).isEqualTo("wide")
    }

    @Test fun `disabled zone is ignored`() {
        val disabled = Fixtures.zone("home", enabled = false)
        val decision = engine.evaluate(listOf(disabled), emptyList(), Fixtures.context())
        assertThat(decision.winner).isNull()
    }

    @Test fun `schedule gates activation`() {
        val workHours = Schedule(
            daysOfWeek = setOf(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
        )
        val work = Fixtures.zone("work", schedule = workHours)

        val duringWork = Fixtures.context(time = LocalTime.of(10, 0), day = DayOfWeek.MONDAY)
        assertThat(engine.evaluate(listOf(work), emptyList(), duringWork).winner?.id).isEqualTo("work")

        val weekend = Fixtures.context(time = LocalTime.of(10, 0), day = DayOfWeek.SUNDAY)
        assertThat(engine.evaluate(listOf(work), emptyList(), weekend).winner).isNull()
    }

    @Test fun `advanced trigger AND logic must all hold`() {
        val gym = Fixtures.zone(
            "gym",
            advancedTrigger = Condition.BluetoothConnected(nameContains = "AirPods"),
        )
        val withoutAirpods = Fixtures.context()
        assertThat(engine.evaluate(listOf(gym), emptyList(), withoutAirpods).winner).isNull()
    }

    @Test fun `standalone rule competes with zones by priority`() {
        val roadTrip = Rule(
            id = "road",
            name = "Road Trip",
            priority = 80,
            condition = Condition.Speed(Comparator.GREATER_THAN, 50.0),
            action = Action.Play(Fixtures.playback("RoadTrip")),
        )
        val home = Fixtures.zone("home", priority = 50)
        val driving = Fixtures.context(speedMph = 70.0)
        val decision = engine.evaluate(listOf(home), listOf(roadTrip), driving)
        assertThat(decision.winner?.id).isEqualTo("road")
        assertThat(decision.winner?.sourceZone).isNull()
    }

    @Test fun `deterministic tie-break by updatedAt when priority and radius equal`() {
        val older = Fixtures.zone("a", radiusMeters = 100.0, priority = 50, updatedAt = 1_000L)
        val newer = Fixtures.zone("b", radiusMeters = 100.0, priority = 50, updatedAt = 2_000L)
        val decision = engine.evaluate(listOf(older, newer), emptyList(), Fixtures.context())
        assertThat(decision.winner?.id).isEqualTo("b")
    }
}
