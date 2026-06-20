package com.spotzones.presentation

import com.google.common.truth.Truth.assertThat
import com.spotzones.automation.AutomationManager
import com.spotzones.domain.backup.BackupManager
import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.model.ThemePreference
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.presentation.screens.settings.SettingsViewModel
import com.spotzones.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val backupManager = mockk<BackupManager>(relaxed = true)
    private val automationManager = mockk<AutomationManager>(relaxed = true)
    private val spotifyAuth = mockk<SpotifyAuth>(relaxed = true)
    private val authCoordinator = mockk<com.spotzones.data.remote.auth.SpotifyAuthCoordinator>(relaxed = true)

    private fun viewModel(): SettingsViewModel {
        every { settingsRepository.settings } returns flowOf(AppSettings.Default)
        every { spotifyAuth.state } returns MutableStateFlow(SpotifyAuthState.Unauthorized)
        return SettingsViewModel(settingsRepository, backupManager, automationManager, spotifyAuth, authCoordinator)
    }

    @Test fun `setTheme writes through to settings repository`() = runTest {
        val transform = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.update(capture(transform)) } returns Unit

        viewModel().setTheme(ThemePreference.DARK)
        advanceUntilIdle()

        val result = transform.captured(AppSettings.Default)
        assertThat(result.themePreference).isEqualTo(ThemePreference.DARK)
    }

    @Test fun `enabling automation starts monitoring`() = runTest {
        viewModel().setAutomationEnabled(true)
        advanceUntilIdle()
        verify { automationManager.startMonitoring() }
    }

    @Test fun `disabling automation stops monitoring`() = runTest {
        viewModel().setAutomationEnabled(false)
        advanceUntilIdle()
        verify { automationManager.stopMonitoring() }
    }

    @Test fun `logout delegates to auth`() = runTest {
        viewModel().logoutSpotify()
        advanceUntilIdle()
        coVerify { spotifyAuth.logout() }
    }
}
