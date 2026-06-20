package com.spotzones.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.data.remote.auth.SpotifyAuthCoordinator
import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.ui.theme.AccentColor
import com.spotzones.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Theme + top-level app state derived from settings and Spotify auth. */
data class AppUiState(
    val loading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: AccentColor = AccentColor.Spotify,
    val amoled: Boolean = true,
    val dynamicColor: Boolean = false,
    val onboardingComplete: Boolean = false,
    val spotifyConnected: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val spotifyAuth: SpotifyAuth,
    private val authCoordinator: SpotifyAuthCoordinator,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(settingsRepository.settings, spotifyAuth.state) { settings, auth ->
            AppUiState(
                loading = false,
                themeMode = settings.themePreference.toUi(),
                accent = AccentColor.fromId(settings.accentColorId),
                amoled = settings.amoledBlack,
                dynamicColor = settings.dynamicColor,
                onboardingComplete = settings.onboardingComplete,
                spotifyConnected = auth is SpotifyAuthState.Authorized,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    /** Called from the activity when a Spotify OAuth redirect arrives. */
    fun onRedirect(uri: Uri) {
        if (!authCoordinator.isRedirect(uri)) return
        viewModelScope.launch { authCoordinator.handleRedirect(uri) }
    }

    private fun com.spotzones.domain.model.ThemePreference.toUi(): ThemeMode = when (this) {
        com.spotzones.domain.model.ThemePreference.SYSTEM -> ThemeMode.System
        com.spotzones.domain.model.ThemePreference.LIGHT -> ThemeMode.Light
        com.spotzones.domain.model.ThemePreference.DARK -> ThemeMode.Dark
    }
}
