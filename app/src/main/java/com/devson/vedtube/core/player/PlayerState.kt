package com.devson.vedtube.core.player

import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream

/**
 * Strongly typed immutable playback states for the Media3 player subsystem.
 */
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Resolving : PlaybackState
    data object Preparing : PlaybackState
    data class Playing(val positionMs: Long, val durationMs: Long) : PlaybackState
    data class Paused(val positionMs: Long, val durationMs: Long) : PlaybackState
    data class Buffering(val positionMs: Long, val durationMs: Long) : PlaybackState
    data object Ended : PlaybackState
    data class Error(val error: AppError) : PlaybackState
}

/**
 * Repeat modes supported by the player subsystem.
 */
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

/**
 * Comprehensive, immutable state of the [VedPlayer] at any instant.
 */
data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentMediaItem: PlaybackSource? = null,
    val currentVideo: Video? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val availableQualities: List<VideoStream> = emptyList(),
    val selectedQuality: VideoStream? = null,
    val queue: List<Video> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isFullscreen: Boolean = false
) {
    val isPlaying: Boolean
        get() = playbackState is PlaybackState.Playing

    val isBuffering: Boolean
        get() = playbackState is PlaybackState.Buffering

    val isResolving: Boolean
        get() = playbackState is PlaybackState.Resolving

    val isPreparing: Boolean
        get() = playbackState is PlaybackState.Preparing

    val isEnded: Boolean
        get() = playbackState is PlaybackState.Ended

    val isError: Boolean
        get() = playbackState is PlaybackState.Error

    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val bufferedFraction: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hasNext: Boolean
        get() = when (repeatMode) {
            RepeatMode.ALL -> queue.isNotEmpty()
            RepeatMode.ONE -> currentQueueIndex in queue.indices
            RepeatMode.OFF -> currentQueueIndex < queue.lastIndex
        }

    val hasPrevious: Boolean
        get() = when (repeatMode) {
            RepeatMode.ALL -> queue.isNotEmpty()
            RepeatMode.ONE -> currentQueueIndex in queue.indices
            RepeatMode.OFF -> currentQueueIndex > 0
        }
}
