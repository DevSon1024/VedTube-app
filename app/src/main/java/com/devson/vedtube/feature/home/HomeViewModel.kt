package com.devson.vedtube.feature.home

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.core.player.PlaybackMediaSourceFactory
import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.data.provider.youtube.url.YoutubeUrlParser
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.VideoStream
import com.devson.vedtube.domain.repository.SettingsRepository
import com.devson.vedtube.domain.resolver.PlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
@OptIn(UnstableApi::class)
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appInfoDao: AppInfoDao,
    private val okHttpClient: OkHttpClient,
    val exoPlayer: ExoPlayer,
    private val playbackResolver: PlaybackResolver,
    private val playbackMediaSourceFactory: PlaybackMediaSourceFactory,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _infrastructureState = MutableStateFlow(
        Triple(
            first = false, // isDatabaseReady
            second = false, // isNetworkReady
            third = false  // isPlayerReady
        )
    )

    private val _parsedMediaState = MutableStateFlow<Pair<String?, ParsedMediaUrl?>>(
        Pair(null, null)
    )

    private val _playbackInternalState = MutableStateFlow(
        PlaybackInternalState()
    )

    private var progressJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.themeSettings,
        _infrastructureState,
        _parsedMediaState,
        _playbackInternalState
    ) { themeSettings, infra, media, playback ->
        HomeUiState(
            themeSettings = themeSettings,
            isDatabaseReady = infra.first,
            isNetworkReady = infra.second,
            isPlayerReady = infra.third,
            rawIncomingUrl = media.first,
            parsedMediaUrl = media.second,
            isLoading = false,
            isResolvingStream = playback.isResolving,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            isCompleted = playback.isCompleted,
            playbackError = playback.error,
            resolvedPlaybackSource = playback.source,
            activeVideoStream = playback.activeStream,
            currentPositionMs = playback.currentPositionMs,
            durationMs = playback.durationMs,
            videoTitle = playback.title,
            uploaderName = playback.uploader
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackInternalState.value = _playbackInternalState.value.copy(
                isPlaying = isPlaying
            )
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isBuffering = true,
                        isCompleted = false
                    )
                }
                Player.STATE_READY -> {
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isBuffering = false,
                        isCompleted = false,
                        durationMs = duration
                    )
                }
                Player.STATE_ENDED -> {
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        isCompleted = true,
                        currentPositionMs = _playbackInternalState.value.durationMs
                    )
                    stopProgressTracker()
                }
                Player.STATE_IDLE -> {
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isBuffering = false
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackInternalState.value = _playbackInternalState.value.copy(
                isBuffering = false,
                isPlaying = false,
                error = error.localizedMessage ?: "Media3 playback failed"
            )
            stopProgressTracker()
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        verifyInfrastructure()
    }

    private fun verifyInfrastructure() {
        viewModelScope.launch {
            val dbReady = withContext(ioDispatcher) {
                try {
                    appInfoDao.insertInfo(AppInfoEntity("app_init", "phase_3_ok"))
                    val info = appInfoDao.getInfo("app_init")
                    info?.value == "phase_3_ok"
                } catch (e: Exception) {
                    false
                }
            }

            val networkReady = try {
                okHttpClient.connectionPool.connectionCount() >= 0
            } catch (e: Exception) {
                false
            }

            val playerReady = try {
                exoPlayer.playbackState >= 0
            } catch (e: Exception) {
                false
            }

            _infrastructureState.value = Triple(dbReady, networkReady, playerReady)
        }
    }

    fun handleIncomingIntent(intentTextOrUrl: String?) {
        if (intentTextOrUrl.isNullOrBlank()) return
        val parsed = YoutubeUrlParser.parse(intentTextOrUrl)
        _parsedMediaState.value = Pair(intentTextOrUrl, parsed)

        when (parsed) {
            is ParsedMediaUrl.Video -> {
                resolveAndPlay(parsed.videoId, parsed.timestampMs)
            }
            else -> {
                // If direct ID passed
                if (YoutubeUrlParser.isValidVideoId(intentTextOrUrl.trim())) {
                    resolveAndPlay(intentTextOrUrl.trim(), null)
                }
            }
        }
    }

    fun resolveAndPlay(videoId: String, startTimestampMs: Long? = null) {
        viewModelScope.launch {
            _playbackInternalState.value = _playbackInternalState.value.copy(
                isResolving = true,
                error = null,
                isCompleted = false
            )

            val result = playbackResolver.resolve(videoId, PlaybackPreferences())
            result.fold(
                onSuccess = { source ->
                    val bestStream = source.selectBestStream()
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isResolving = false,
                        source = source,
                        activeStream = bestStream,
                        title = source.title,
                        uploader = source.uploaderName,
                        durationMs = source.durationMs ?: 0L,
                        error = null
                    )

                    withContext(ioDispatcher) {
                        try {
                            val mediaSource = playbackMediaSourceFactory.createMediaSource(source)
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                exoPlayer.setMediaSource(mediaSource)
                                if (startTimestampMs != null && startTimestampMs > 0) {
                                    exoPlayer.seekTo(startTimestampMs)
                                }
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        } catch (e: Exception) {
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                _playbackInternalState.value = _playbackInternalState.value.copy(
                                    error = "MediaSource creation error: ${e.message}"
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    _playbackInternalState.value = _playbackInternalState.value.copy(
                        isResolving = false,
                        error = error.message ?: "Failed to resolve media stream"
                    )
                }
            )
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_playbackInternalState.value.isCompleted) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackInternalState.value = _playbackInternalState.value.copy(
            currentPositionMs = positionMs
        )
    }

    fun retryPlayback() {
        val videoId = _playbackInternalState.value.source?.videoId
            ?: (_parsedMediaState.value.second as? ParsedMediaUrl.Video)?.videoId
        if (!videoId.isNullOrBlank()) {
            resolveAndPlay(videoId, null)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && exoPlayer.isPlaying) {
                val currentPos = exoPlayer.currentPosition
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _playbackInternalState.value.durationMs
                _playbackInternalState.value = _playbackInternalState.value.copy(
                    currentPositionMs = currentPos,
                    durationMs = duration
                )
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun setThemeConfig(config: AppThemeConfig) {
        viewModelScope.launch {
            settingsRepository.setThemeConfig(config)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    private data class PlaybackInternalState(
        val isResolving: Boolean = false,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val isCompleted: Boolean = false,
        val error: String? = null,
        val source: PlaybackSource? = null,
        val activeStream: VideoStream? = null,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val title: String? = null,
        val uploader: String? = null
    )
}
