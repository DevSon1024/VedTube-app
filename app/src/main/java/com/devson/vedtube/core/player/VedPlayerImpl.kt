package com.devson.vedtube.core.player

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream
import com.devson.vedtube.domain.resolver.PlaybackResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust Singleton implementation of [VedPlayer] and [PlayerController].
 * Manages the single ExoPlayer instance lifecycle, asynchronous stream resolution,
 * continuous playback position updates, track selection, and multi-tier queueing.
 */
@Singleton
@OptIn(UnstableApi::class)
class VedPlayerImpl @Inject constructor(
    override val exoPlayer: ExoPlayer,
    private val playbackResolver: PlaybackResolver,
    private val mediaItemFactory: MediaItemFactory,
    override val queueManager: QueueManager,
    private val playerErrorMapper: PlayerErrorMapper,
    private val watchHistoryRepository: WatchHistoryRepository,
    @Dispatcher(VedTubeDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : VedPlayer {

    private val playerScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var positionTickerJob: Job? = null
    private var resolveJob: Job? = null

    private var activePlaybackSource: PlaybackSource? = null
    private var lastSavedVolume: Float = 1.0f
    private var lastSavedHistoryTimestamp = 0L

    private val exoListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackStateFromExo()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
            updatePlaybackStateFromExo()
        }

        override fun onPlayerError(error: PlaybackException) {
            stopPositionTicker()
            val mappedError = playerErrorMapper.map(error)
            _playerState.update { current ->
                current.copy(playbackState = PlaybackState.Error(mappedError))
            }
        }
    }

    init {
        exoPlayer.addListener(exoListener)

        // Observe queue changes to keep PlayerState in sync
        queueManager.queue.onEach { queueList ->
            _playerState.update { it.copy(queue = queueList) }
        }.launchIn(playerScope)

        queueManager.currentIndex.onEach { index ->
            _playerState.update { it.copy(currentQueueIndex = index) }
        }.launchIn(playerScope)

        queueManager.currentVideo.onEach { video ->
            _playerState.update { it.copy(currentVideo = video) }
        }.launchIn(playerScope)

        queueManager.isShuffleEnabled.onEach { shuffle ->
            _playerState.update { it.copy(isShuffleEnabled = shuffle) }
        }.launchIn(playerScope)

        queueManager.repeatMode.onEach { repeat ->
            _playerState.update { it.copy(repeatMode = repeat) }
        }.launchIn(playerScope)
    }

    override fun handleEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.PlayVideo -> playVideo(event.video, event.preferences)
            is PlayerEvent.PlayVideoId -> playVideoId(event.videoId, event.title, event.uploader, event.preferences)
            is PlayerEvent.Play -> play()
            is PlayerEvent.Pause -> pause()
            is PlayerEvent.TogglePlayPause -> togglePlayPause()
            is PlayerEvent.SeekTo -> seekTo(event.positionMs)
            is PlayerEvent.SeekForward -> seekForward(event.offsetMs)
            is PlayerEvent.SeekBackward -> seekBackward(event.offsetMs)
            is PlayerEvent.SetPlaybackSpeed -> setSpeed(event.speed)
            is PlayerEvent.SetVolume -> setVolume(event.volume)
            is PlayerEvent.SetMuted -> setMuted(event.muted)
            is PlayerEvent.SetRepeatMode -> setRepeatMode(event.repeatMode)
            is PlayerEvent.ToggleRepeatMode -> toggleRepeatMode()
            is PlayerEvent.SetShuffle -> setShuffle(event.enabled)
            is PlayerEvent.ToggleShuffle -> toggleShuffle()
            is PlayerEvent.SelectQuality -> selectQuality(event.quality)
            is PlayerEvent.Next -> next()
            is PlayerEvent.Previous -> previous()
            is PlayerEvent.Enqueue -> enqueue(event.video)
            is PlayerEvent.EnqueueAll -> enqueueAll(event.videos)
            is PlayerEvent.RemoveFromQueue -> removeFromQueue(event.index)
            is PlayerEvent.MoveInQueue -> moveInQueue(event.fromIndex, event.toIndex)
            is PlayerEvent.ClearQueue -> clearQueue()
            is PlayerEvent.PlayQueueIndex -> playQueueIndex(event.index)
            is PlayerEvent.SetFullscreen -> setFullscreen(event.fullscreen)
            is PlayerEvent.ToggleFullscreen -> toggleFullscreen()
            is PlayerEvent.Retry -> retry()
            is PlayerEvent.Stop -> stop()
            is PlayerEvent.Release -> release()
        }
    }

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    override fun pause() {
        saveCurrentWatchProgress()
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val target = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        updatePositionImmediately(target)
    }

    override fun seekForward(offsetMs: Long) {
        val current = exoPlayer.currentPosition
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val target = if (duration > 0) (current + offsetMs).coerceAtMost(duration) else current + offsetMs
        seekTo(target)
    }

    override fun seekBackward(offsetMs: Long) {
        val current = exoPlayer.currentPosition
        val target = (current - offsetMs).coerceAtLeast(0L)
        seekTo(target)
    }

    override fun setSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
        exoPlayer.setPlaybackSpeed(clampedSpeed)
        _playerState.update { it.copy(playbackSpeed = clampedSpeed) }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        lastSavedVolume = if (clamped > 0f) clamped else lastSavedVolume
        _playerState.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    override fun setMuted(muted: Boolean) {
        if (muted) {
            lastSavedVolume = if (exoPlayer.volume > 0f) exoPlayer.volume else lastSavedVolume
            exoPlayer.volume = 0f
            _playerState.update { it.copy(volume = 0f, isMuted = true) }
        } else {
            val restoreVol = if (lastSavedVolume > 0f) lastSavedVolume else 1.0f
            exoPlayer.volume = restoreVol
            _playerState.update { it.copy(volume = restoreVol, isMuted = false) }
        }
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        queueManager.setRepeatMode(repeatMode)
    }

    override fun toggleRepeatMode(): RepeatMode {
        return queueManager.toggleRepeatMode()
    }

    override fun setShuffle(enabled: Boolean) {
        queueManager.setShuffle(enabled)
    }

    override fun toggleShuffle(): Boolean {
        return queueManager.toggleShuffle()
    }

    override fun selectQuality(quality: VideoStream) {
        val source = activePlaybackSource ?: return
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        val mediaSource = mediaItemFactory.createMediaSource(
            playbackSource = source,
            overrideQuality = quality
        )
        exoPlayer.setMediaSource(mediaSource, currentPos)
        exoPlayer.prepare()
        if (wasPlaying) {
            exoPlayer.play()
        }
        _playerState.update { it.copy(selectedQuality = quality) }
    }

    override fun next() {
        val nextVideo = queueManager.next()
        if (nextVideo != null) {
            playVideoInternal(nextVideo)
        } else if (_playerState.value.repeatMode == RepeatMode.ALL && queueManager.queue.value.isNotEmpty()) {
            val first = queueManager.setIndex(0)
            if (first != null) playVideoInternal(first)
        } else {
            stop()
        }
    }

    override fun previous() {
        if (exoPlayer.currentPosition > 3000L) {
            // If already played more than 3 seconds, restart current video
            seekTo(0)
            return
        }
        val prevVideo = queueManager.previous()
        if (prevVideo != null) {
            playVideoInternal(prevVideo)
        } else {
            seekTo(0)
        }
    }

    override fun playVideo(video: Video, preferences: PlaybackPreferences) {
        queueManager.setQueue(listOf(video), startIndex = 0)
        playVideoInternal(video, preferences)
    }

    override fun playVideoId(
        videoId: String,
        title: String?,
        uploader: String?,
        preferences: PlaybackPreferences
    ) {
        val video = Video(
            id = videoId,
            title = title ?: "Video $videoId",
            uploaderName = uploader ?: "Unknown"
        )
        playVideo(video, preferences)
    }

    override fun enqueue(video: Video) {
        queueManager.add(video)
    }

    override fun enqueueAll(videos: List<Video>) {
        queueManager.addAll(videos)
    }

    override fun removeFromQueue(index: Int) {
        queueManager.remove(index)
    }

    override fun moveInQueue(fromIndex: Int, toIndex: Int) {
        queueManager.move(fromIndex, toIndex)
    }

    override fun clearQueue() {
        queueManager.clear()
    }

    override fun playQueueIndex(index: Int) {
        val video = queueManager.setIndex(index)
        if (video != null) {
            playVideoInternal(video)
        }
    }

    override fun setFullscreen(fullscreen: Boolean) {
        _playerState.update { it.copy(isFullscreen = fullscreen) }
    }

    override fun toggleFullscreen() {
        _playerState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    override fun retry() {
        val current = _playerState.value.currentVideo
        if (current != null) {
            playVideoInternal(current)
        }
    }

    override fun stop() {
        saveCurrentWatchProgress()
        resolveJob?.cancel()
        stopPositionTicker()
        exoPlayer.stop()
        _playerState.update {
            it.copy(
                playbackState = PlaybackState.Idle,
                currentPositionMs = 0L,
                bufferedPositionMs = 0L
            )
        }
    }

    override fun release() {
        stop()
        exoPlayer.removeListener(exoListener)
        exoPlayer.release()
    }

    private fun playVideoInternal(
        video: Video,
        preferences: PlaybackPreferences = PlaybackPreferences()
    ) {
        resolveJob?.cancel()
        stopPositionTicker()

        _playerState.update { current ->
            current.copy(
                currentVideo = video,
                playbackState = PlaybackState.Resolving,
                currentPositionMs = 0L,
                bufferedPositionMs = 0L,
                durationMs = if (video.durationSeconds > 0) video.durationSeconds * 1000L else 0L,
                availableQualities = emptyList(),
                selectedQuality = null
            )
        }

        resolveJob = playerScope.launch {
            val result = withContext(ioDispatcher) {
                playbackResolver.resolve(video.id, preferences)
            }

            result.fold(
                onSuccess = { source ->
                    activePlaybackSource = source
                    val selectedStream = source.selectBestStream(preferences)

                    _playerState.update { current ->
                        current.copy(
                            currentMediaItem = source,
                            playbackState = PlaybackState.Preparing,
                            availableQualities = source.streams,
                            selectedQuality = selectedStream,
                            durationMs = source.durationMs ?: current.durationMs
                        )
                    }

                    try {
                        val savedProgress = withContext(ioDispatcher) {
                            watchHistoryRepository.getProgress(video.id)
                        } ?: 0L

                        val mediaSource = mediaItemFactory.createMediaSource(source, selectedStream, preferences)
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()

                        val effectiveDuration = source.durationMs ?: (if (video.durationSeconds > 0) video.durationSeconds * 1000L else 0L)
                        if (savedProgress > 1000L && (effectiveDuration <= 0L || savedProgress < effectiveDuration * 0.95)) {
                            exoPlayer.seekTo(savedProgress)
                        }

                        exoPlayer.play()
                    } catch (e: Throwable) {
                        val mappedError = playerErrorMapper.map(e)
                        _playerState.update { it.copy(playbackState = PlaybackState.Error(mappedError)) }
                    }
                },
                onFailure = { error ->
                    val appError = if (error is AppError) error else AppError.ContentUnavailable(error.message ?: "Failed to resolve video stream")
                    _playerState.update { it.copy(playbackState = PlaybackState.Error(appError)) }
                }
            )
        }
    }

    private fun saveCurrentWatchProgress() {
        val currentVideo = _playerState.value.currentVideo ?: return
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.coerceAtLeast(0L).let { if (it > 0) it else currentVideo.durationSeconds * 1000L }

        if (pos > 1000L) {
            playerScope.launch(ioDispatcher) {
                watchHistoryRepository.saveProgress(
                    videoId = currentVideo.id,
                    title = currentVideo.title,
                    channelName = currentVideo.uploaderName,
                    thumbnailUrl = currentVideo.thumbnailUrl ?: "https://i.ytimg.com/vi/${currentVideo.id}/hqdefault.jpg",
                    durationMs = dur,
                    progressMs = pos
                )
            }
        }
    }

    private fun updatePlaybackStateFromExo() {
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val position = exoPlayer.currentPosition.coerceAtLeast(0L)
        val buffered = exoPlayer.bufferedPosition.coerceAtLeast(0L)

        val newPlaybackState: PlaybackState = when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> {
                if (exoPlayer.playerError != null) {
                    PlaybackState.Error(playerErrorMapper.map(exoPlayer.playerError!!))
                } else if (_playerState.value.playbackState is PlaybackState.Resolving) {
                    PlaybackState.Resolving
                } else {
                    PlaybackState.Idle
                }
            }
            Player.STATE_BUFFERING -> PlaybackState.Buffering(position, duration)
            Player.STATE_READY -> {
                if (exoPlayer.playWhenReady) {
                    PlaybackState.Playing(position, duration)
                } else {
                    PlaybackState.Paused(position, duration)
                }
            }
            Player.STATE_ENDED -> {
                saveCurrentWatchProgress()
                handlePlaybackEnded()
                PlaybackState.Ended
            }
            else -> PlaybackState.Idle
        }

        _playerState.update { current ->
            current.copy(
                playbackState = newPlaybackState,
                currentPositionMs = position,
                durationMs = if (duration > 0) duration else current.durationMs,
                bufferedPositionMs = buffered
            )
        }
    }

    private fun handlePlaybackEnded() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (queueManager.hasNext()) {
                    next()
                }
            }
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = playerScope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                _playerState.update { current ->
                    current.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else current.durationMs,
                        bufferedPositionMs = buf
                    )
                }

                val now = System.currentTimeMillis()
                if (now - lastSavedHistoryTimestamp >= 5000L) {
                    lastSavedHistoryTimestamp = now
                    saveCurrentWatchProgress()
                }

                delay(250)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun updatePositionImmediately(positionMs: Long) {
        _playerState.update { current ->
            current.copy(currentPositionMs = positionMs)
        }
    }
}
