package com.spotzones.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.ArtistResult
import com.spotzones.domain.spotify.SpotifyCatalog
import com.spotzones.domain.spotify.TrackSearchResult
import com.spotzones.domain.model.PlaylistRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class SearchResults(
    val zones: List<Zone> = emptyList(),
    val playlists: List<PlaylistRef> = emptyList(),
    val tracks: List<TrackSearchResult> = emptyList(),
    val artists: List<ArtistResult> = emptyList(),
    val loading: Boolean = false,
) {
    val isEmpty: Boolean get() = zones.isEmpty() && playlists.isEmpty() && tracks.isEmpty() && artists.isEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val catalog: SpotifyCatalog,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    private val allZones: StateFlow<List<Zone>> =
        zoneRepository.observeZones().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Debounce remote search so we don't hammer the Spotify API on every keystroke.
        _query
            .debounce(300)
            .distinctUntilChanged()
            .onEach { runSearch(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) { _query.value = value }

    private suspend fun runSearch(q: String) {
        if (q.isBlank()) {
            _results.value = SearchResults()
            return
        }
        val zones = allZones.value.filter { it.name.contains(q, ignoreCase = true) }
        _results.value = SearchResults(zones = zones, loading = true)

        val playlists = catalog.searchPlaylists(q).getOrNull().orEmpty()
        val tracks = catalog.searchTracks(q).getOrNull().orEmpty()
        val artists = catalog.searchArtists(q).getOrNull().orEmpty()

        // Guard against an out-of-order response for a stale query.
        if (_query.value == q) {
            _results.value = SearchResults(zones, playlists, tracks, artists, loading = false)
        }
    }
}
