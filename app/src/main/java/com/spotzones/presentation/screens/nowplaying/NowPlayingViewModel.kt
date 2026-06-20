package com.spotzones.presentation.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.automation.AutomationStateStore
import com.spotzones.domain.model.PlaybackState
import com.spotzones.domain.model.RepeatMode
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val playback: PlaybackState = PlaybackState.Disconnected,
    val activeZone: Zone? = null,
    val upcomingZone: Zone? = null,
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controller: SpotifyController,
    zoneRepository: ZoneRepository,
    stateStore: AutomationStateStore,
) : ViewModel() {

    val uiState: StateFlow<NowPlayingUiState> = combine(
        controller.playbackState,
        zoneRepository.observeZones(),
        stateStore.activeCandidateId,
    ) { playback, zones, activeId ->
        val active = zones.firstOrNull { it.id == activeId }
        NowPlayingUiState(
            playback = playback,
            activeZone = active,
            // Simple heuristic for "upcoming": highest-priority other enabled zone.
            upcomingZone = zones.filter { it.enabled && it.id != activeId }.maxByOrNull { it.priority },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

    init {
        viewModelScope.launch { controller.connect() }
    }

    fun togglePlayPause() = viewModelScope.launch { controller.togglePlayPause() }
    fun skipNext() = viewModelScope.launch { controller.skipNext() }
    fun skipPrevious() = viewModelScope.launch { controller.skipPrevious() }
    fun seekTo(ms: Long) = viewModelScope.launch { controller.seekTo(ms) }
    fun toggleShuffle() = viewModelScope.launch { controller.setShuffle(!uiState.value.playback.shuffle) }
    fun cycleRepeat() = viewModelScope.launch {
        val next = when (uiState.value.playback.repeatMode) {
            RepeatMode.OFF -> RepeatMode.CONTEXT
            RepeatMode.CONTEXT -> RepeatMode.TRACK
            RepeatMode.TRACK -> RepeatMode.OFF
        }
        controller.setRepeat(next)
    }
}
