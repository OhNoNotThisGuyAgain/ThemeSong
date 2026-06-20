package com.spotzones.presentation.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.domain.model.GeofenceSensitivity
import com.spotzones.domain.model.ThemePreference
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.presentation.components.SectionHeader
import com.spotzones.presentation.util.launchSpotifyConnect
import com.spotzones.ui.theme.AccentColor
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text != null) viewModel.importBackup(text)
    }

    val spotifyAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* redirect handled by MainActivity */
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.Message -> snackbar.showSnackbar(effect.text)
                is SettingsEffect.Share -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, effect.payload)
                    }
                    context.startActivity(Intent.createChooser(share, "Export PlayZones backup"))
                }
            }
        }
    }

    androidx.compose.material3.Scaffold(snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            item { Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 8.dp)) }

            item { SectionHeader("Spotify") }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val authorized = authState as? SpotifyAuthState.Authorized
                            Text(if (authorized != null) "Connected" else "Not connected", style = MaterialTheme.typography.titleMedium)
                            Text(
                                authorized?.displayName ?: "Sign in to control playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (authState is SpotifyAuthState.Authorized) {
                            OutlinedButton(onClick = viewModel::logoutSpotify) { Text("Log out") }
                        } else {
                            androidx.compose.material3.Button(
                                onClick = {
                                    launchSpotifyConnect(
                                        context,
                                        viewModel::buildSpotifyAuthIntent,
                                        spotifyAuthLauncher,
                                    )
                                },
                            ) { Text("Connect") }
                        }
                    }
                }
            }

            item { SectionHeader("Appearance") }
            item { ThemeRow(settings.themePreference, viewModel::setTheme) }
            item { AccentRow(settings.accentColorId, viewModel::setAccent) }
            item { ToggleRow("AMOLED black", "Pure black backgrounds save battery on OLED", settings.amoledBlack, viewModel::setAmoled) }
            item { ToggleRow("Material You colours", "Match your wallpaper (Android 12+)", settings.dynamicColor, viewModel::setDynamicColor) }

            item { SectionHeader("Automation") }
            item { ToggleRow("Automation enabled", "Let zones control your music", settings.automationEnabled, viewModel::setAutomationEnabled) }
            item { ToggleRow("Only with headphones", "Skip automation on the speaker", settings.requireHeadphonesForAuto, viewModel::setRequireHeadphones) }
            item { SensitivityRow(settings.geofenceSensitivity, viewModel::setSensitivity) }

            item { SectionHeader("Notifications") }
            item { ToggleRow("Transition notifications", "Notify when zone music changes", settings.showTransitionNotifications, viewModel::setTransitionNotifications) }

            item { SectionHeader("Backup") }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::exportBackup, modifier = Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(onClick = { importLauncher.launch("application/json") }, modifier = Modifier.weight(1f)) { Text("Import") }
                }
            }

            item { SectionHeader("Privacy") }
            item {
                ToggleRow(
                    "Anonymous analytics",
                    "Off by default. Sends only anonymous usage events (no account, no location) under a random id you can reset by turning this off.",
                    settings.analyticsEnabled,
                    viewModel::setAnalytics,
                )
            }

            item { Spacer(Modifier.size(32.dp)) }
            item { Text("PlayZones · v${com.spotzones.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeRow(current: ThemePreference, onChange: (ThemePreference) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreference.entries.forEach { pref ->
            FilterChip(selected = current == pref, onClick = { onChange(pref) }, label = { Text(pref.name.lowercase().replaceFirstChar { it.uppercase() }) })
        }
    }
}

@Composable
private fun AccentRow(currentId: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentColor.entries.forEach { accent ->
            val selected = accent.id == currentId
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(accent.seed)
                    .border(width = if (selected) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                    .clickable { onChange(accent.id) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SensitivityRow(current: GeofenceSensitivity, onChange: (GeofenceSensitivity) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Geofence sensitivity", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            GeofenceSensitivity.entries.forEach { s ->
                FilterChip(
                    selected = current == s,
                    onClick = { onChange(s) },
                    label = { Text(s.name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }) },
                )
            }
        }
    }
}
