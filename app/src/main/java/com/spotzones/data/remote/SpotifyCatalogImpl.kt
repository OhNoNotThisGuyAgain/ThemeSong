package com.spotzones.data.remote

import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.remote.api.SpotifyApiService
import com.spotzones.data.remote.dto.PlaylistDto
import com.spotzones.domain.model.PlaylistRef
import com.spotzones.domain.spotify.ArtistResult
import com.spotzones.domain.spotify.SpotifyCatalog
import com.spotzones.domain.spotify.TrackSearchResult
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class SpotifyCatalogImpl @Inject constructor(
    private val api: SpotifyApiService,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SpotifyCatalog {

    override suspend fun userPlaylists(): Outcome<List<PlaylistRef>> = call {
        // Page through the library so large collections are fully available to the picker.
        buildList {
            var offset = 0
            while (true) {
                val page = api.userPlaylists(limit = 50, offset = offset)
                addAll(page.items.map { it.toRef() })
                if (page.next == null || page.items.isEmpty()) break
                offset += page.items.size
                if (offset > 1000) break // safety bound
            }
        }
    }

    override suspend fun playlist(uri: String): Outcome<PlaylistRef> = call {
        api.playlist(uri.substringAfterLast(':')).toRef()
    }

    override suspend fun searchPlaylists(query: String): Outcome<List<PlaylistRef>> = call {
        api.search(query, type = "playlist").playlists?.items?.map { it.toRef() }.orEmpty()
    }

    override suspend fun searchTracks(query: String): Outcome<List<TrackSearchResult>> = call {
        api.search(query, type = "track").tracks?.items?.map { track ->
            TrackSearchResult(
                uri = track.uri,
                title = track.name,
                artists = track.artists.joinToString(", ") { it.name },
                album = track.album?.name.orEmpty(),
                imageUrl = track.album?.images?.firstOrNull()?.url,
            )
        }.orEmpty()
    }

    override suspend fun searchArtists(query: String): Outcome<List<ArtistResult>> = call {
        api.search(query, type = "artist").artists?.items?.map { artist ->
            ArtistResult(artist.uri, artist.name, artist.images.firstOrNull()?.url)
        }.orEmpty()
    }

    private fun PlaylistDto.toRef() = PlaylistRef(
        uri = uri,
        name = name,
        imageUrl = images.firstOrNull()?.url,
        ownerName = owner?.displayName,
        trackCount = tracks?.total,
    )

    private suspend fun <T> call(block: suspend () -> T): Outcome<T> = withContext(io) {
        try {
            Outcome.Success(block())
        } catch (e: IOException) {
            Outcome.Failure(DomainError.NoNetwork)
        } catch (e: HttpException) {
            if (e.code() == 401) Outcome.Failure(DomainError.SpotifyNotAuthorized)
            else Outcome.Failure(DomainError.SpotifyRemote("Spotify API error ${e.code()}"))
        } catch (e: Exception) {
            Outcome.Failure(DomainError.Unexpected(e.message ?: "Unknown error", e))
        }
    }
}
