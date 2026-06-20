package com.spotzones.presentation.screens.zoneeditor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.domain.model.RepeatMode
import com.spotzones.domain.model.TransitionMode
import com.spotzones.domain.model.Weekday
import com.spotzones.presentation.components.LoadingState
import com.spotzones.presentation.components.SectionHeader
import com.spotzones.presentation.util.minuteOfDayToText
import kotlinx.coroutines.flow.collectLatest

private val ZoneColors = listOf(
    0xFF1DB954, 0xFF3DD5C8, 0xFFFFB951, 0xFF8B7CFF, 0xFFFF6F9C, 0xFF4FC3F7, 0xFFEF5350, 0xFFAED581,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditorScreen(onDone: () -> Unit, viewModel: ZoneEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ZoneEditorEvent.Saved, ZoneEditorEvent.Deleted -> onDone()
                is ZoneEditorEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is ZoneEditorEvent.Share -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.payload)
                    }
                    context.startActivity(Intent.createChooser(share, "Share zone"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New zone" else "Edit zone") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = viewModel::duplicate) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate") }
                        IconButton(onClick = viewModel::export) { Icon(Icons.Outlined.Share, contentDescription = "Export") }
                        IconButton(onClick = viewModel::delete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
                    }
                },
            )
        },
    ) { inner ->
        if (state.loading) {
            LoadingState(Modifier.padding(inner))
            return@Scaffold
        }

        Column(
            Modifier.padding(inner).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                label = { Text("Zone name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader("Colour")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZoneColors.forEach { hex ->
                    val selected = state.colorHex == hex
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(hex))
                            .border(width = if (selected) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                            .clickable { viewModel.update { it.copy(colorHex = hex) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }

            SectionHeader("Playlist")
            Card(onClick = { showPlaylistPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.playlist?.name ?: "Choose a playlist", style = MaterialTheme.typography.titleMedium)
                        state.playlist?.ownerName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary)
                }
            }

            SectionHeader("Geofence")
            LabeledSlider("Radius", "${state.radiusMeters.toInt()} m", state.radiusMeters, 20f..2000f) { v -> viewModel.update { it.copy(radiusMeters = v) } }
            LabeledSlider("Priority", state.priority.toInt().toString(), state.priority, 1f..100f) { v -> viewModel.update { it.copy(priority = v) } }

            SectionHeader("Playback")
            SettingSwitch("Shuffle", state.shuffle) { viewModel.update { s -> s.copy(shuffle = it) } }
            RepeatSelector(state.repeatMode) { viewModel.update { s -> s.copy(repeatMode = it) } }
            TransitionSelector(state.transitionMode) { viewModel.update { s -> s.copy(transitionMode = it) } }
            if (state.transitionMode == TransitionMode.CROSSFADE) {
                LabeledSlider("Crossfade", "${state.crossfadeSeconds.toInt()} s", state.crossfadeSeconds, 0f..12f) { v -> viewModel.update { it.copy(crossfadeSeconds = v) } }
            }
            LabeledSlider("Volume", "${state.volumePercent.toInt()}%", state.volumePercent, 0f..100f) { v -> viewModel.update { it.copy(volumePercent = v) } }
            SettingSwitch("Fade in volume", state.fadeIn) { viewModel.update { s -> s.copy(fadeIn = it) } }
            SettingSwitch("Resume where I left off", state.resumePosition) { viewModel.update { s -> s.copy(resumePosition = it) } }

            SectionHeader("Schedule")
            SettingSwitch("Active all day", state.allDay) { viewModel.update { s -> s.copy(allDay = it) } }
            if (!state.allDay) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("From", state.startMinute, Modifier.weight(1f)) { viewModel.update { s -> s.copy(startMinute = it) } }
                    TimeField("To", state.endMinute, Modifier.weight(1f)) { viewModel.update { s -> s.copy(endMinute = it) } }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Weekday.entries.forEach { day ->
                    FilterChip(
                        selected = day in state.days,
                        onClick = { viewModel.toggleDay(day) },
                        label = { Text(day.name.take(3)) },
                    )
                }
            }

            SectionHeader("Advanced triggers")
            Text("Only activate this zone when…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch("Headphones connected", state.requireHeadphones) { viewModel.update { s -> s.copy(requireHeadphones = it) } }
            SettingSwitch("Phone is charging", state.requireCharging) { viewModel.update { s -> s.copy(requireCharging = it) } }
            SettingSwitch("I'm driving", state.requireDriving) { viewModel.update { s -> s.copy(requireDriving = it) } }

            SettingSwitch("Zone enabled", state.enabled) { viewModel.update { s -> s.copy(enabled = it) } }

            Spacer(Modifier.size(8.dp))
            Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) {
                Text(if (state.isNew) "Create zone" else "Save changes")
            }
            Spacer(Modifier.size(24.dp))
        }
    }

    if (showPlaylistPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showPlaylistPicker = false }, sheetState = sheetState) {
            PlaylistPickerContent(
                state = state,
                onPick = { ref -> viewModel.update { it.copy(playlist = ref) }; showPlaylistPicker = false },
                onRetry = viewModel::loadPlaylists,
            )
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, sliderValue: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = sliderValue, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatSelector(mode: RepeatMode, onChange: (RepeatMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        RepeatMode.entries.forEachIndexed { index, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onChange(m) },
                shape = SegmentedButtonDefaults.itemShape(index, RepeatMode.entries.size),
            ) { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransitionSelector(mode: TransitionMode, onChange: (TransitionMode) -> Unit) {
    val labels = mapOf(
        TransitionMode.IMMEDIATE to "Immediate",
        TransitionMode.FINISH_SONG to "Finish song",
        TransitionMode.CROSSFADE to "Crossfade",
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TransitionMode.entries.forEachIndexed { index, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onChange(m) },
                shape = SegmentedButtonDefaults.itemShape(index, TransitionMode.entries.size),
            ) { Text(labels[m] ?: m.name) }
        }
    }
}

@Composable
private fun TimeField(label: String, minute: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    // Tap to advance in 30-minute steps; keeps the editor dependency-free while remaining usable.
    Card(onClick = { onChange((minute + 30) % (24 * 60)) }, modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(minuteOfDayToText(minute), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PlaylistPickerContent(state: ZoneEditorState, onPick: (com.spotzones.domain.model.PlaylistRef) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Choose a playlist", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        when {
            state.playlistsLoading -> LoadingState(Modifier.size(120.dp))
            state.availablePlaylists.isEmpty() -> {
                Text("No playlists found. Make sure you're connected to Spotify.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
            else -> androidx.compose.foundation.lazy.LazyColumn {
                items(state.availablePlaylists.size) { i ->
                    val ref = state.availablePlaylists[i]
                    Row(Modifier.fillMaxWidth().clickable { onPick(ref) }.padding(vertical = 12.dp)) {
                        Column {
                            Text(ref.name, style = MaterialTheme.typography.titleMedium)
                            Text("${ref.trackCount ?: 0} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
