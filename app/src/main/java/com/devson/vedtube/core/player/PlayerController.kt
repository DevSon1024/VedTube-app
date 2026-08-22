package com.devson.vedtube.core.player

import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream

/**
 * Interface defining all playback control commands and queue operations.
 */
interface PlayerController {
    fun handleEvent(event: PlayerEvent)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekForward(offsetMs: Long = 10_000L)
    fun seekBackward(offsetMs: Long = 10_000L)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
    fun setRepeatMode(repeatMode: RepeatMode)
    fun toggleRepeatMode(): RepeatMode
    fun setShuffle(enabled: Boolean)
    fun toggleShuffle(): Boolean
    fun selectQuality(quality: VideoStream)
    fun toggleSubtitles(): Boolean
    fun selectSubtitle(subtitle: com.devson.vedtube.domain.model.SubtitleTrack?)
    fun dismissSponsorNotification()
    fun next()
    fun previous()
    fun playVideo(video: Video, preferences: PlaybackPreferences = PlaybackPreferences())
    fun playVideoId(videoId: String, title: String? = null, uploader: String? = null, preferences: PlaybackPreferences = PlaybackPreferences())
    fun enqueue(video: Video)
    fun enqueueAll(videos: List<Video>)
    fun removeFromQueue(index: Int)
    fun moveInQueue(fromIndex: Int, toIndex: Int)
    fun clearQueue()
    fun playQueueIndex(index: Int)
    fun setFullscreen(fullscreen: Boolean)
    fun toggleFullscreen()
    fun retry()
    fun stop()
    fun release()
}
