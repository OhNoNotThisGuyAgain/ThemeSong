package com.spotzones.automation

import com.google.common.truth.Truth.assertThat
import com.spotzones.domain.engine.RuleEngine
import com.spotzones.domain.location.ContextProvider
import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.ManualOverride
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.repository.HistoryRepository
import com.spotzones.domain.repository.RuleRepository
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyController
import com.spotzones.domain.util.Outcome
import com.spotzones.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AutomationCoordinatorTest {

    private lateinit var zoneRepository: ZoneRepository
    private lateinit var ruleRepository: RuleRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var contextProvider: ContextProvider
    private lateinit var spotifyController: SpotifyController
    private lateinit var stateStore: AutomationStateStore
    private lateinit var coordinator: AutomationCoordinator

    private val activeId = MutableStateFlow<String?>(null)

    @Before fun setup() {
        zoneRepository = mockk(relaxed = true)
        ruleRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        contextProvider = mockk()
        spotifyController = mockk(relaxed = true)
        stateStore = mockk(relaxed = true)

        every { stateStore.activeCandidateId } returns activeId
        every { stateStore.lastActiveZoneId } returns activeId.value
        coEvery { ruleRepository.getEnabledRules() } returns emptyList()
        coEvery { historyRepository.closeOpenEntry(any(), any(), any()) } returns Unit
        coEvery { spotifyController.connect() } returns Outcome.Success(Unit)
        coEvery { spotifyController.apply(any()) } returns Outcome.Success(Unit)

        coordinator = AutomationCoordinator(
            zoneRepository, ruleRepository, settingsRepository, historyRepository,
            contextProvider, spotifyController, stateStore, RuleEngine(),
        )
    }

    @Test fun `disabled automation performs no playback`() = runTest {
        coEvery { settingsRepository.current() } returns AppSettings.Default.copy(automationEnabled = false)
        coEvery { settingsRepository.currentOverride() } returns null

        val result = coordinator.evaluate()

        assertThat(result.changed).isFalse()
        coVerify(exactly = 0) { spotifyController.apply(any()) }
    }

    @Test fun `active override suppresses automation`() = runTest {
        coEvery { settingsRepository.current() } returns AppSettings.Default
        coEvery { settingsRepository.currentOverride() } returns ManualOverride(OverrideMode.PERMANENT, expiresAt = null)

        val result = coordinator.evaluate()

        assertThat(result.suppressedByOverride).isTrue()
        coVerify(exactly = 0) { spotifyController.apply(any()) }
    }

    @Test fun `entering a zone applies its playback and records history`() = runTest {
        val home = Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 300.0)
        coEvery { settingsRepository.current() } returns AppSettings.Default
        coEvery { settingsRepository.currentOverride() } returns null
        coEvery { zoneRepository.getEnabledZones() } returns listOf(home)
        coEvery { contextProvider.currentContext() } returns Fixtures.context(location = GeoCoordinate(40.0, -73.0))

        val result = coordinator.evaluate()

        assertThat(result.changed).isTrue()
        assertThat(result.activeZoneName).isEqualTo("home")
        coVerify(exactly = 1) { spotifyController.apply(any<PlaybackConfig>()) }
        coVerify(exactly = 1) { historyRepository.record(any()) }
        io.mockk.verify { stateStore.update("home", "home") }
    }

    @Test fun `re-evaluating the same winner is idempotent`() = runTest {
        val home = Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 300.0)
        activeId.value = "home" // already active
        every { stateStore.lastActiveZoneId } returns "home"
        coEvery { settingsRepository.current() } returns AppSettings.Default
        coEvery { settingsRepository.currentOverride() } returns null
        coEvery { zoneRepository.getEnabledZones() } returns listOf(home)
        coEvery { contextProvider.currentContext() } returns Fixtures.context(location = GeoCoordinate(40.0, -73.0))

        val result = coordinator.evaluate()

        assertThat(result.changed).isFalse()
        coVerify(exactly = 0) { spotifyController.apply(any()) }
    }

    @Test fun `leaving all zones pauses playback`() = runTest {
        activeId.value = "home"
        every { stateStore.lastActiveZoneId } returns "home"
        coEvery { settingsRepository.current() } returns AppSettings.Default
        coEvery { settingsRepository.currentOverride() } returns null
        coEvery { zoneRepository.getEnabledZones() } returns listOf(
            Fixtures.zone("home", center = GeoCoordinate(40.0, -73.0), radiusMeters = 100.0),
        )
        // Now far outside the zone.
        coEvery { contextProvider.currentContext() } returns Fixtures.context(location = GeoCoordinate(41.0, -73.0))

        val result = coordinator.evaluate()

        assertThat(result.changed).isTrue()
        coVerify { spotifyController.pause() }
    }
}
