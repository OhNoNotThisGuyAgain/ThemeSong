package com.spotzones.presentation.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.presentation.util.launchSpotifyConnect

/**
 * Progressive onboarding. Permissions are requested one purpose at a time, each preceded by an
 * in-context explanation, so the user is never hit with a wall of system dialogs — directly
 * addressing the "request progressively, explain clearly, never overwhelm" requirement.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val spotifyConnected = authState is SpotifyAuthState.Authorized

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* redirect handled by MainActivity */ }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun connectSpotify() = launchSpotifyConnect(context, viewModel::buildSpotifyAuthIntent, authLauncher)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.size(24.dp))
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text("Welcome to PlayZones", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            "Your music, on autopilot. PlayZones changes what Spotify plays based on where you are and what you're doing — your playlists and account stay exactly as they are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(32.dp))

        OnboardingStep(
            icon = Icons.Outlined.MusicNote,
            title = "Connect Spotify",
            description = "Sign in so PlayZones can control playback. We never see your password.",
            done = spotifyConnected,
            actionLabel = "Connect",
            enabled = !spotifyConnected,
            onAction = { connectSpotify() },
        )

        OnboardingStep(
            icon = Icons.Outlined.LocationOn,
            title = "Allow location",
            description = "Used only to detect your zones. For switching music while the app is closed, choose \"Allow all the time\".",
            actionLabel = "Allow",
            onAction = {
                locationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnboardingStep(
                icon = Icons.Outlined.Notifications,
                title = "Allow notifications",
                description = "Shows the active zone and quick controls to pause automation.",
                actionLabel = "Allow",
                onAction = { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }

        Spacer(Modifier.size(32.dp))
        Button(
            onClick = viewModel::finish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
        ) {
            Text("Get started")
        }
        TextButton(onClick = viewModel::finish) { Text("Skip for now") }
    }
}

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    done: Boolean = false,
    enabled: Boolean = true,
) {
    androidx.compose.material3.ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            if (!done) {
                OutlinedButton(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}
