package com.spotzones.data

import com.google.common.truth.Truth.assertThat
import com.spotzones.core.serialization.AppJson
import com.spotzones.data.local.entity.toDomain
import com.spotzones.data.local.entity.toEntity
import com.spotzones.domain.model.Action
import com.spotzones.domain.model.Comparator
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.MovementState
import com.spotzones.testutil.Fixtures
import kotlinx.serialization.encodeToString
import org.junit.Test

/**
 * Guards the persistence/backup contract: the polymorphic Condition/Action trees and full Zone must
 * survive a JSON round-trip unchanged, since the same serializer backs Room columns and export files.
 */
class SerializationRoundTripTest {

    @Test fun `condition tree round-trips through json`() {
        val condition: Condition = Condition.All(
            listOf(
                Condition.InsideZone("zone-1"),
                Condition.TimeWindow(17 * 60, 20 * 60),
                Condition.Any(
                    listOf(
                        Condition.BluetoothConnected(nameContains = "AirPods"),
                        Condition.Speed(Comparator.GREATER_THAN, 50.0),
                    ),
                ),
                Condition.Not(Condition.Moving(setOf(MovementState.STATIONARY))),
            ),
        )
        val json = AppJson.encodeToString(condition)
        val restored = AppJson.decodeFromString<Condition>(json)
        assertThat(restored).isEqualTo(condition)
    }

    @Test fun `action round-trips`() {
        val action: Action = Action.Play(Fixtures.playback("Focus"))
        val restored = AppJson.decodeFromString<Action>(AppJson.encodeToString(action))
        assertThat(restored).isEqualTo(action)
    }

    @Test fun `zone entity mapping is lossless`() {
        val zone = Fixtures.zone(
            id = "z1",
            schedule = com.spotzones.domain.model.Schedule(startMinuteOfDay = 60, endMinuteOfDay = 120),
            advancedTrigger = Condition.Charging(true),
        )
        val restored = zone.toEntity().toDomain()
        assertThat(restored).isEqualTo(zone)
    }
}
