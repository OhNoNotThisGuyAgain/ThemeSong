package com.spotzones.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.model.SpotifyConnectionState
import com.spotzones.domain.model.Zone
import com.spotzones.presentation.components.SectionHeader
import com.spotzones.presentation.util.relativeTime

@Composable
fun DashboardScreen(
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenMap: () -> Unit,
    onEditZone: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SpotZones", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onOpenSearch) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
            }
        }

        item {
            NowPlayingCard(
                state = state,
                onClick = onOpenNowPlaying,
                onPlayPause = viewModel::togglePlayPause,
                onSkip = viewModel::skipNext,
            )
        }

        item { ActiveZoneCard(zone = state.activeZone, onOpenMap = onOpenMap) }

        item {
            AutomationControlCard(
                paused = state.automationPaused,
                onPause = { viewModel.pauseAutomation(OverrideMode.ONE_HOUR) },
                onResume = viewModel::resumeAutomation,
            )
        }

        if (state.recentTransitions.isNotEmpty()) {
            item { SectionHeader("Recent transitions") }
            items(state.recentTransitions, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.zoneName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                entry.playlistName ?: "Automation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(relativeTime(entry.enteredAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionHeader("Your zones") }
        items(state.zones, key = { it.id }) { zone ->
            ZoneRow(zone = zone, active = zone.id == state.activeZone?.id, onClick = { onEditZone(zone.id) })
        }
        item {
            AssistChip(
                onClick = onOpenMap,
                label = { Text("Add a zone") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    state: DashboardUiState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val art = state.playback.track?.imageUri
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) {
                    AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                val track = state.playback.track
                Text(
                    track?.title ?: connectionLabel(state.playback.connection),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track?.artists ?: "Open Spotify to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(onClick = onPlayPause) {
                Icon(
                    if (state.playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.playback.isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = onSkip) { Icon(Icons.Filled.SkipNext, contentDescription = "Skip") }
        }
    }
}

@Composable
private fun ActiveZoneCard(zone: Zone?, onOpenMap: () -> Unit) {
    Card(onClick = onOpenMap, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(if (zone != null) Color(zone.colorHex) else MaterialTheme.colorScheme.outline))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Current zone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(zone?.name ?: "Not in a zone", style = MaterialTheme.typography.titleMedium)
            }
            if (zone != null) {
                Text("${zone.radiusMeters.toInt()} m · P${zone.priority}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AutomationControlCard(paused: Boolean, onPause: () -> Unit, onResume: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (paused) "Automation paused" else "Automation on", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (paused) "Music won't change automatically" else "Zones are controlling your music",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (paused) {
                androidx.compose.material3.Button(onClick = onResume) { Text("Resume") }
            } else {
                androidx.compose.material3.OutlinedButton(onClick = onPause) { Text("Pause 1h") }
            }
        }
    }
}

@Composable
private fun ZoneRow(zone: Zone, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(zone.colorHex)))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.titleSmall)
            Text(
                zone.playback.playlist?.name ?: "No playlist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (active) {
            AssistChip(onClick = onClick, label = { Text("Active") })
        } else if (!zone.enabled) {
            Text("Off", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun connectionLabel(state: SpotifyConnectionState): String = when (state) {
    SpotifyConnectionState.Connected -> "Nothing playing"
    SpotifyConnectionState.Connecting -> "Connecting to Spotify…"
    SpotifyConnectionState.Disconnected -> "Not connected"
    SpotifyConnectionState.NotInstalled -> "Spotify not installed"
    is SpotifyConnectionState.Failed -> "Spotify unavailable"
}
