package com.spotzones.presentation.screens.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.automation.AutomationManager
import com.spotzones.data.remote.auth.SpotifyAuthCoordinator
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authCoordinator: SpotifyAuthCoordinator,
    private val automationManager: AutomationManager,
    spotifyAuth: SpotifyAuth,
) : ViewModel() {

    val authState: StateFlow<SpotifyAuthState> = spotifyAuth.state
    val spotifyConfigured: Boolean get() = authCoordinator.isConfigured

    fun buildSpotifyAuthIntent(): Intent = authCoordinator.buildAuthIntent()

    fun finish() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingComplete = true) }
            automationManager.startMonitoring()
        }
    }
}
