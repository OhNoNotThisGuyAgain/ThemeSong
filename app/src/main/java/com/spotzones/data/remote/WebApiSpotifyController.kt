package com.spotzones.data.remote

import com.spotzones.core.coroutines.ApplicationScope
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.remote.api.SpotifyApiService
import com.spotzones.data.remote.dto.OffsetDto
import com.spotzones.data.remote.dto.PlayRequestDto
import com.spotzones.data.remote.dto.PlayerStateDto
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.PlaybackState
import com.spotzones.domain.model.PlaylistPosition
import com.spotzones.domain.model.RepeatMode
import com.spotzones.domain.model.SpotifyConnectionState
import com.spotzones.domain.model.TrackInfo
import com.spotzones.domain.model.TransitionMode
import com.spotzones.domain.repository.PlaybackPositionRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyAuthState
import com.spotzones.domain.spotify.SpotifyController
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

/**
 * [SpotifyController] backed by the Spotify Web API "Connect" player endpoints.
 *
 * Why Web API rather than App Remote here: it compiles and runs with only open-source dependencies
 * (no proprietary `.aar`), works across any active Connect device, and fully satisfies the
 * controller port. Because the rest of the app depends only on [SpotifyController], an App Remote
 * implementation can replace this with a single DI binding change (see README → "Swapping to App Remote").
 *
 * Bidirectional sync is achieved by polling player state on a low-frequency loop while the Now
 * Playing surface is active, plus an immediate refresh after every command. Between polls, progress
 * is interpolated locally so the seek bar moves smoothly without extra network calls.
 */
@Singleton
class WebApiSpotifyController @Inject constructor(
    private val api: SpotifyApiService,
    private val auth: SpotifyAuth,
    private val positionRepository: PlaybackPositionRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SpotifyController {

    private val _playbackState = MutableStateFlow(PlaybackState.Disconnected)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var pollJob: Job? = null
    private var lastPolledAt = 0L

    override suspend fun connect(): Outcome<Unit> = withContext(io) {
        when (auth.state.value) {
            is SpotifyAuthState.Authorized -> {
                _playbackState.value = _playbackState.value.copy(connection = SpotifyConnectionState.Connecting)
                refreshNow()
                startPolling()
                Outcome.Success(Unit)
            }
            else -> {
                _playbackState.value = PlaybackState.Disconnected
                Outcome.Failure(DomainError.SpotifyNotAuthorized)
            }
        }
    }

    override fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        _playbackState.value = PlaybackState.Disconnected
    }

    override suspend fun apply(config: PlaybackConfig): Outcome<Unit> = command {
        val playlist = config.playlist ?: return@command
        // Resume saved position for this context if requested, else start fresh.
        val saved = if (config.resumePlaybackPosition) positionRepository.get(playlist.uri) else null
        val body = PlayRequestDto(
            contextUri = playlist.uri,
            positionMs = saved?.positionMs,
            offset = saved?.let { OffsetDto(uri = it.trackUri) },
        )

        if (config.transitionMode == TransitionMode.CROSSFADE && config.fadeInVolume) {
            api.volume(0).ensureSuccess()
        }
        api.shuffle(config.shuffle).ensureSuccess()
        api.repeat(config.repeatMode.toApi()).ensureSuccess()
        api.play(body).ensureSuccess()

        if (config.transitionMode == TransitionMode.CROSSFADE && config.fadeInVolume) {
            rampVolume(to = config.volumePercent, seconds = config.crossfadeSeconds)
        } else {
            api.volume(config.volumePercent).ensureSuccess()
        }
    }

    override suspend fun resume(): Outcome<Unit> = command { api.resume().ensureSuccess() }
    override suspend fun pause(): Outcome<Unit> = command {
        saveCurrentPosition()
        api.pause().ensureSuccess()
    }
    override suspend fun skipNext(): Outcome<Unit> = command { api.next().ensureSuccess() }
    override suspend fun skipPrevious(): Outcome<Unit> = command { api.previous().ensureSuccess() }
    override suspend fun seekTo(positionMs: Long): Outcome<Unit> = command { api.seek(positionMs).ensureSuccess() }
    override suspend fun setShuffle(enabled: Boolean): Outcome<Unit> = command { api.shuffle(enabled).ensureSuccess() }
    override suspend fun setRepeat(mode: RepeatMode): Outcome<Unit> = command { api.repeat(mode.toApi()).ensureSuccess() }

    override suspend fun togglePlayPause(): Outcome<Unit> = command {
        if (_playbackState.value.isPlaying) {
            saveCurrentPosition()
            api.pause().ensureSuccess()
        } else {
            api.resume().ensureSuccess()
        }
    }

    // --- internals ---

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch(io) {
            while (isActive) {
                refreshNow()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshNow() {
        try {
            val response = api.playerState()
            if (response.code() == 204 || response.body() == null) {
                // No active device — connected to Spotify, but nothing is playing here.
                _playbackState.value = _playbackState.value.copy(
                    connection = SpotifyConnectionState.Connected,
                    isPlaying = false,
                    deviceName = null,
                )
            } else {
                _playbackState.value = response.body()!!.toPlaybackState()
            }
            lastPolledAt = System.currentTimeMillis()
        } catch (e: IOException) {
            _playbackState.value = _playbackState.value.copy(
                connection = SpotifyConnectionState.Failed("No connection"),
            )
        } catch (e: HttpException) {
            if (e.code() == 401) {
                _playbackState.value = PlaybackState.Disconnected
            }
        } catch (e: Exception) {
            Timber.w("Player state refresh failed: %s", e.message)
        }
    }

    /** Linear volume ramp used to emulate a smooth fade-in on transitions. */
    private suspend fun rampVolume(to: Int, seconds: Int) {
        val steps = (seconds.coerceAtLeast(1) * 2).coerceAtMost(24)
        for (i in 1..steps) {
            val level = (to * i / steps).coerceIn(0, 100)
            runCatching { api.volume(level) }
            delay((seconds * 1000L) / steps)
        }
    }

    private suspend fun saveCurrentPosition() {
        val state = _playbackState.value
        val context = state.contextUri ?: return
        val track = state.track ?: return
        positionRepository.save(
            PlaylistPosition(
                contextUri = context,
                trackUri = track.uri,
                positionMs = state.positionMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private inline fun retrofit2.Response<Unit>.ensureSuccess() {
        if (!isSuccessful && code() != 204) throw HttpException(this)
    }

    private suspend fun command(block: suspend () -> Unit): Outcome<Unit> = withContext(io) {
        try {
            block()
            refreshNow()
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(DomainError.NoNetwork)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Outcome.Failure(DomainError.SpotifyNotAuthorized)
                403 -> Outcome.Failure(DomainError.SpotifyRemote("Spotify Premium is required for remote control."))
                404 -> Outcome.Failure(DomainError.SpotifyRemote("No active Spotify device. Open Spotify and play something once."))
                else -> Outcome.Failure(DomainError.SpotifyRemote("Spotify error ${e.code()}"))
            }
        } catch (e: Exception) {
            Outcome.Failure(DomainError.Unexpected(e.message ?: "Playback error", e))
        }
    }

    private fun RepeatMode.toApi(): String = when (this) {
        RepeatMode.OFF -> "off"
        RepeatMode.TRACK -> "track"
        RepeatMode.CONTEXT -> "context"
    }

    private fun PlayerStateDto.toPlaybackState(): PlaybackState = PlaybackState(
        connection = SpotifyConnectionState.Connected,
        isPlaying = isPlaying,
        track = item?.let {
            TrackInfo(
                uri = it.uri,
                title = it.name,
                artists = it.artists.joinToString(", ") { a -> a.name },
                album = it.album?.name.orEmpty(),
                imageUri = it.album?.images?.firstOrNull()?.url,
                durationMs = it.durationMs,
            )
        },
        positionMs = progressMs,
        shuffle = shuffleState,
        repeatMode = when (repeatState) {
            "track" -> RepeatMode.TRACK
            "context" -> RepeatMode.CONTEXT
            else -> RepeatMode.OFF
        },
        contextUri = context?.uri,
        contextName = null,
        deviceName = device?.name,
    )

    private companion object {
        const val POLL_INTERVAL_MS = 4_000L
    }
}
