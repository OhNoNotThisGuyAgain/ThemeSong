package com.spotzones.presentation.screens.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spotzones.domain.model.RepeatMode
import com.spotzones.presentation.util.formatTrackTime
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val track = state.playback.track

    // Interpolate position locally between the controller's polled snapshots for a smooth seek bar.
    var displayedPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(track?.uri, state.playback.positionMs, state.playback.isPlaying) {
        displayedPosition = state.playback.positionMs
        while (state.playback.isPlaying && track != null && displayedPosition < track.durationMs) {
            delay(500)
            displayedPosition = (displayedPosition + 500).coerceAtMost(track.durationMs)
        }
    }
    var scrubbing by remember { mutableFloatStateOf(-1f) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Zone context chips.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.activeZone?.let { zone ->
                AssistChip(
                    onClick = {},
                    label = { Text(zone.name) },
                    leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(zone.colorHex))) },
                )
            }
            Spacer(Modifier.weight(1f))
            Text(state.playback.deviceName ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(0.5f))

        // Album art with a subtle play/pause scale.
        val scale by animateFloatAsState(if (state.playback.isPlaying) 1f else 0.94f, label = "art-scale")
        Box(
            Modifier.fillMaxWidth(0.85f).aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (track?.imageUri != null) {
                AsyncImage(model = track.imageUri, contentDescription = "Album art", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.weight(0.5f))

        Text(
            track?.title ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track?.artists ?: "Open Spotify and press play",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.size(16.dp))

        val duration = track?.durationMs ?: 1L
        val sliderValue = if (scrubbing >= 0f) scrubbing else (displayedPosition.toFloat() / duration).coerceIn(0f, 1f)
        Slider(
            value = sliderValue,
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                viewModel.seekTo((scrubbing * duration).toLong())
                scrubbing = -1f
            },
            enabled = track != null,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTrackTime((sliderValue * duration).toLong()), style = MaterialTheme.typography.labelMedium)
            Text(formatTrackTime(duration), style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.size(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (state.playback.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = viewModel::skipPrevious) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp)) }
            FilledIconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(
                    if (state.playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.playback.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = viewModel::skipNext) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp)) }
            IconButton(onClick = viewModel::cycleRepeat) {
                Icon(
                    if (state.playback.repeatMode == RepeatMode.TRACK) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.playback.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
