package com.devson.vedtube.core.player

import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream

/**
 * User actions and system events dispatched to [PlayerController] / [VedPlayer].
 */
sealed interface PlayerEvent {
    data class PlayVideo(
        val video: Video,
        val preferences: PlaybackPreferences = PlaybackPreferences()
    ) : PlayerEvent

    data class PlayVideoId(
        val videoId: String,
        val title: String? = null,
        val uploader: String? = null,
        val preferences: PlaybackPreferences = PlaybackPreferences()
    ) : PlayerEvent

    data object Play : PlayerEvent
    data object Pause : PlayerEvent
    data object TogglePlayPause : PlayerEvent

    data class SeekTo(val positionMs: Long) : PlayerEvent
    data class SeekForward(val offsetMs: Long = 10_000L) : PlayerEvent
    data class SeekBackward(val offsetMs: Long = 10_000L) : PlayerEvent

    data class SetPlaybackSpeed(val speed: Float) : PlayerEvent
    data class SetVolume(val volume: Float) : PlayerEvent
    data class SetMuted(val muted: Boolean) : PlayerEvent

    data class SetRepeatMode(val repeatMode: RepeatMode) : PlayerEvent
    data object ToggleRepeatMode : PlayerEvent

    data class SetShuffle(val enabled: Boolean) : PlayerEvent
    data object ToggleShuffle : PlayerEvent

    data class SelectQuality(val quality: VideoStream) : PlayerEvent

    data object Next : PlayerEvent
    data object Previous : PlayerEvent

    data class Enqueue(val video: Video) : PlayerEvent
    data class EnqueueAll(val videos: List<Video>) : PlayerEvent
    data class RemoveFromQueue(val index: Int) : PlayerEvent
    data class MoveInQueue(val fromIndex: Int, val toIndex: Int) : PlayerEvent
    data object ClearQueue : PlayerEvent
    data class PlayQueueIndex(val index: Int) : PlayerEvent

    data class SetFullscreen(val fullscreen: Boolean) : PlayerEvent
    data object ToggleFullscreen : PlayerEvent

    data object Retry : PlayerEvent
    data object Stop : PlayerEvent
    data object Release : PlayerEvent
}
