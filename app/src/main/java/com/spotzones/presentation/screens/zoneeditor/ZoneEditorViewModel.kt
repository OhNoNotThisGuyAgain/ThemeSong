package com.spotzones.presentation.screens.zoneeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.MovementState
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.PlaylistRef
import com.spotzones.domain.model.RepeatMode
import com.spotzones.domain.model.Schedule
import com.spotzones.domain.model.TransitionMode
import com.spotzones.domain.model.Weekday
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyCatalog
import com.spotzones.domain.spotify.TrackSearchResult
import com.spotzones.domain.util.onFailure
import com.spotzones.domain.util.onSuccess
import com.spotzones.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Mutable editor model — a flat, Compose-friendly mirror of [Zone] kept in one immutable state. */
data class ZoneEditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val id: String? = null,
    val name: String = "",
    val colorHex: Long = 0xFF1DB954,
    val center: GeoCoordinate = GeoCoordinate(0.0, 0.0),
    val radiusMeters: Float = 200f,
    val priority: Float = 50f,
    val enabled: Boolean = true,
    val playlist: PlaylistRef? = null,
    val shuffle: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.CONTEXT,
    val transitionMode: TransitionMode = TransitionMode.CROSSFADE,
    val crossfadeSeconds: Float = 6f,
    val volumePercent: Float = 80f,
    val fadeIn: Boolean = true,
    val resumePosition: Boolean = true,
    val allDay: Boolean = true,
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 17 * 60,
    val days: Set<Weekday> = Weekday.entries.toSet(),
    val requireHeadphones: Boolean = false,
    val requireCharging: Boolean = false,
    val requireDriving: Boolean = false,
    val availablePlaylists: List<PlaylistRef> = emptyList(),
    val playlistsLoading: Boolean = false,
    val trackQuery: String = "",
    val trackResults: List<TrackSearchResult> = emptyList(),
    val tracksLoading: Boolean = false,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !saving
}

sealed interface ZoneEditorEvent {
    data object Saved : ZoneEditorEvent
    data object Deleted : ZoneEditorEvent
    data class Message(val text: String) : ZoneEditorEvent
    data class Share(val payload: String) : ZoneEditorEvent
}

@HiltViewModel
class ZoneEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val zoneRepository: ZoneRepository,
    private val settingsRepository: SettingsRepository,
    private val catalog: SpotifyCatalog,
    private val backupManager: com.spotzones.domain.backup.BackupManager,
    private val analytics: com.spotzones.domain.analytics.Analytics,
) : ViewModel() {

    private val zoneId: String? = savedState.get<String>(Routes.ZONE_EDITOR_ARG_ID)?.takeIf { it != "new" }
    private val seedLat = savedState.get<String>(Routes.ZONE_EDITOR_ARG_LAT)?.toDoubleOrNull()?.takeIf { !it.isNaN() }
    private val seedLng = savedState.get<String>(Routes.ZONE_EDITOR_ARG_LNG)?.toDoubleOrNull()?.takeIf { !it.isNaN() }

    private val _state = MutableStateFlow(ZoneEditorState())
    val state: StateFlow<ZoneEditorState> = _state.asStateFlow()

    private val _events = Channel<ZoneEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val settings = settingsRepository.current()
        if (zoneId != null) {
            val zone = zoneRepository.getZone(zoneId)
            if (zone != null) {
                _state.value = zone.toEditorState()
                return@launch
            }
        }
        _state.value = ZoneEditorState(
            loading = false,
            isNew = true,
            center = GeoCoordinate(seedLat ?: 0.0, seedLng ?: 0.0),
            crossfadeSeconds = settings.defaultCrossfadeSeconds.toFloat(),
            volumePercent = settings.defaultVolumePercent.toFloat(),
        )
        loadPlaylists()
    }

    fun loadPlaylists() = viewModelScope.launch {
        _state.update { it.copy(playlistsLoading = true) }
        catalog.userPlaylists()
            .onSuccess { list -> _state.update { it.copy(availablePlaylists = list, playlistsLoading = false) } }
            .onFailure { error ->
                _state.update { it.copy(playlistsLoading = false) }
                _events.send(ZoneEditorEvent.Message(error.message))
            }
    }

    private var trackJob: Job? = null

    /** Debounced song search for the "Songs" tab of the source picker. */
    fun searchTracks(query: String) {
        _state.update { it.copy(trackQuery = query) }
        trackJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(trackResults = emptyList(), tracksLoading = false) }
            return
        }
        trackJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(tracksLoading = true) }
            catalog.searchTracks(query)
                .onSuccess { list -> _state.update { it.copy(trackResults = list, tracksLoading = false) } }
                .onFailure { _state.update { it.copy(tracksLoading = false) } }
        }
    }

    /** Selects a single track as the zone's source (uri `spotify:track:…`). */
    fun pickTrack(track: TrackSearchResult) {
        _state.update {
            it.copy(
                playlist = PlaylistRef(
                    uri = track.uri,
                    name = track.title,
                    imageUrl = track.imageUrl,
                    ownerName = track.artists,
                    trackCount = null,
                ),
            )
        }
    }

    fun update(transform: (ZoneEditorState) -> ZoneEditorState) = _state.update(transform)

    fun toggleDay(day: Weekday) = _state.update { s ->
        val days = if (day in s.days) s.days - day else s.days + day
        s.copy(days = days.ifEmpty { setOf(day) })
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (!s.canSave) return@launch
        _state.update { it.copy(saving = true) }
        val zone = s.toZone()
        val wasNew = s.isNew
        val triggerCount = listOf(s.requireHeadphones, s.requireCharging, s.requireDriving).count { it }
        zoneRepository.upsert(zone)
            .onSuccess {
                analytics.track(
                    if (wasNew) com.spotzones.domain.analytics.AnalyticsEvent.ZoneCreated(triggerCount, !s.allDay)
                    else com.spotzones.domain.analytics.AnalyticsEvent.ZoneEdited(viaMap = false),
                )
                _events.send(ZoneEditorEvent.Saved)
            }
            .onFailure { _events.send(ZoneEditorEvent.Message(it.message)); _state.update { st -> st.copy(saving = false) } }
    }

    fun delete() = viewModelScope.launch {
        val id = _state.value.id ?: return@launch
        zoneRepository.delete(id).onSuccess {
            analytics.track(com.spotzones.domain.analytics.AnalyticsEvent.ZoneDeleted)
            _events.send(ZoneEditorEvent.Deleted)
        }
    }

    fun duplicate() = viewModelScope.launch {
        val copy = _state.value.toZone().copy(
            id = java.util.UUID.randomUUID().toString(),
            name = _state.value.name + " copy",
        )
        zoneRepository.upsert(copy).onSuccess { _events.send(ZoneEditorEvent.Saved) }
    }

    fun export() = viewModelScope.launch {
        val id = _state.value.id ?: run { _events.send(ZoneEditorEvent.Message("Save the zone first")); return@launch }
        backupManager.exportZone(id)
            .onSuccess { _events.send(ZoneEditorEvent.Share(it)) }
            .onFailure { _events.send(ZoneEditorEvent.Message(it.message)) }
    }

    // --- mapping ---

    private fun Zone.toEditorState(): ZoneEditorState {
        val trigger = advancedTrigger
        val conditions = (trigger as? Condition.All)?.conditions ?: listOfNotNull(trigger.takeIf { it != Condition.Always })
        return ZoneEditorState(
            loading = false,
            isNew = false,
            id = id,
            name = name,
            colorHex = colorHex,
            center = center,
            radiusMeters = radiusMeters.toFloat(),
            priority = priority.toFloat(),
            enabled = enabled,
            playlist = playback.playlist,
            shuffle = playback.shuffle,
            repeatMode = playback.repeatMode,
            transitionMode = playback.transitionMode,
            crossfadeSeconds = playback.crossfadeSeconds.toFloat(),
            volumePercent = playback.volumePercent.toFloat(),
            fadeIn = playback.fadeInVolume,
            resumePosition = playback.resumePlaybackPosition,
            allDay = schedule.isAllDay,
            startMinute = schedule.startMinuteOfDay,
            endMinute = schedule.endMinuteOfDay,
            days = schedule.daysOfWeek,
            requireHeadphones = conditions.any { it is Condition.HeadphonesConnected },
            requireCharging = conditions.any { it is Condition.Charging && it.charging },
            requireDriving = conditions.any { it is Condition.Moving && MovementState.DRIVING in it.states },
        ).also { viewModelScope.launch { loadPlaylists() } }
    }

    private fun ZoneEditorState.toZone(): Zone {
        val advanced = buildList {
            if (requireHeadphones) add(Condition.HeadphonesConnected(true))
            if (requireCharging) add(Condition.Charging(true))
            if (requireDriving) add(Condition.Moving(setOf(MovementState.DRIVING)))
        }
        val trigger: Condition = when {
            advanced.isEmpty() -> Condition.Always
            advanced.size == 1 -> advanced.first()
            else -> Condition.All(advanced)
        }
        return Zone(
            id = id ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex,
            center = center,
            radiusMeters = radiusMeters.toDouble(),
            priority = priority.toInt().coerceIn(1, 100),
            enabled = enabled,
            playback = PlaybackConfig(
                playlist = playlist,
                shuffle = shuffle,
                repeatMode = repeatMode,
                transitionMode = transitionMode,
                crossfadeSeconds = crossfadeSeconds.toInt(),
                volumePercent = volumePercent.toInt(),
                fadeInVolume = fadeIn,
                resumePlaybackPosition = resumePosition,
            ),
            schedule = if (allDay) {
                Schedule(daysOfWeek = days)
            } else {
                Schedule(daysOfWeek = days, startMinuteOfDay = startMinute, endMinuteOfDay = endMinute)
            },
            advancedTrigger = trigger,
        )
    }

    private fun MutableStateFlow<ZoneEditorState>.update(transform: (ZoneEditorState) -> ZoneEditorState) {
        value = transform(value)
    }
}
