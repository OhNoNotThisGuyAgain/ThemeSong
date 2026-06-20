package com.spotzones.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.automation.AutomationManager
import com.spotzones.domain.backup.BackupManager
import com.spotzones.domain.backup.ImportMode
import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.model.GeofenceSensitivity
import com.spotzones.domain.model.ThemePreference
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.domain.util.onFailure
import com.spotzones.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEffect {
    data class Share(val payload: String) : SettingsEffect
    data class Message(val text: String) : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val automationManager: AutomationManager,
    private val spotifyAuth: SpotifyAuth,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    val authState: StateFlow<SpotifyAuthState> = spotifyAuth.state

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { settingsRepository.update(transform) }

    fun setTheme(theme: ThemePreference) = update { it.copy(themePreference = theme) }
    fun setAccent(id: String) = update { it.copy(accentColorId = id) }
    fun setAmoled(value: Boolean) = update { it.copy(amoledBlack = value) }
    fun setDynamicColor(value: Boolean) = update { it.copy(dynamicColor = value) }
    fun setTransitionNotifications(value: Boolean) = update { it.copy(showTransitionNotifications = value) }
    fun setRequireHeadphones(value: Boolean) = update { it.copy(requireHeadphonesForAuto = value) }
    fun setAnalytics(value: Boolean) = update { it.copy(analyticsEnabled = value) }
    fun setSensitivity(value: GeofenceSensitivity) = update { it.copy(geofenceSensitivity = value) }

    fun setAutomationEnabled(value: Boolean) {
        update { it.copy(automationEnabled = value) }
        if (value) automationManager.startMonitoring() else automationManager.stopMonitoring()
    }

    fun exportBackup() = viewModelScope.launch {
        backupManager.export()
            .onSuccess { _effects.send(SettingsEffect.Share(it)) }
            .onFailure { _effects.send(SettingsEffect.Message(it.message)) }
    }

    fun importBackup(payload: String) = viewModelScope.launch {
        backupManager.import(payload, ImportMode.MERGE)
            .onSuccess { _effects.send(SettingsEffect.Message("Imported ${it.zonesImported} zones, ${it.rulesImported} rules")) }
            .onFailure { _effects.send(SettingsEffect.Message(it.message)) }
    }

    fun logoutSpotify() = viewModelScope.launch { spotifyAuth.logout() }
}
