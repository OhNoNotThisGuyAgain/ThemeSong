package com.spotzones.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.automation.AutomationManager
import com.spotzones.automation.AutomationStateStore
import com.spotzones.domain.model.ManualOverride
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.model.PlaybackState
import com.spotzones.domain.model.TransitionHistory
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.HistoryRepository
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val playback: PlaybackState = PlaybackState.Disconnected,
    val activeZone: Zone? = null,
    val zones: List<Zone> = emptyList(),
    val override: ManualOverride? = null,
    val recentTransitions: List<TransitionHistory> = emptyList(),
) {
    val automationPaused: Boolean get() = override != null
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    zoneRepository: ZoneRepository,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    stateStore: AutomationStateStore,
    private val automationManager: AutomationManager,
    private val controller: SpotifyController,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        zoneRepository.observeZones(),
        controller.playbackState,
        settingsRepository.override,
        historyRepository.observeRecent(5),
        stateStore.activeCandidateId,
    ) { zones, playback, override, recent, activeId ->
        DashboardUiState(
            playback = playback,
            activeZone = zones.firstOrNull { it.id == activeId },
            zones = zones,
            override = override,
            recentTransitions = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun pauseAutomation(mode: OverrideMode) = viewModelScope.launch {
        automationManager.pauseAutomation(mode, uiState.value.activeZone?.id)
    }

    fun resumeAutomation() = viewModelScope.launch { automationManager.resumeAutomation() }

    fun togglePlayPause() = viewModelScope.launch { controller.togglePlayPause() }
    fun skipNext() = viewModelScope.launch { controller.skipNext() }
}
