package com.spotzones.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.presentation.navigation.SpotZonesRoot
import com.spotzones.ui.theme.SpotZonesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash on screen only until settings/auth have loaded — avoids a theme flash.
        splash.setKeepOnScreenCondition { viewModel.uiState.value.loading }

        // Handle a cold-start OAuth redirect.
        intent?.data?.let(viewModel::onRedirect)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SpotZonesTheme(
                themeMode = state.themeMode,
                accent = state.accent,
                dynamicColor = state.dynamicColor,
                amoled = state.amoled,
            ) {
                SpotZonesRoot(appState = state)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Warm-start OAuth redirect (singleTask launch mode routes it here).
        intent.data?.let(viewModel::onRedirect)
        setIntent(intent)
    }
}
