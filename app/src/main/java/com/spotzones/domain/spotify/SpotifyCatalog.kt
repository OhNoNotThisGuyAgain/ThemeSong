package com.spotzones.domain.spotify

import com.spotzones.domain.model.PlaylistRef
import com.spotzones.domain.util.Outcome

/** A track returned from search. */
data class TrackSearchResult(
    val uri: String,
    val title: String,
    val artists: String,
    val album: String,
    val imageUrl: String?,
)

/** An artist returned from search. */
data class ArtistResult(
    val uri: String,
    val name: String,
    val imageUrl: String?,
)

/** Read-only access to the user's Spotify catalog via the Web API, used by pickers and search. */
interface SpotifyCatalog {
    suspend fun userPlaylists(): Outcome<List<PlaylistRef>>
    suspend fun playlist(uri: String): Outcome<PlaylistRef>
    suspend fun searchPlaylists(query: String): Outcome<List<PlaylistRef>>
    suspend fun searchTracks(query: String): Outcome<List<TrackSearchResult>>
    suspend fun searchArtists(query: String): Outcome<List<ArtistResult>>
}
