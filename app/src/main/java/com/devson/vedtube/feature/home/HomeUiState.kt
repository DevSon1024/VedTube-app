package com.devson.vedtube.feature.home

import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.VideoStream

data class HomeUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDatabaseReady: Boolean = false,
    val isNetworkReady: Boolean = false,
    val isPlayerReady: Boolean = false,
    val rawIncomingUrl: String? = null,
    val parsedMediaUrl: ParsedMediaUrl? = null,
    val isLoading: Boolean = false,

    // Real Stream Playback State
    val isResolvingStream: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isCompleted: Boolean = false,
    val playbackError: String? = null,
    val resolvedPlaybackSource: PlaybackSource? = null,
    val activeVideoStream: VideoStream? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val videoTitle: String? = null,
    val uploaderName: String? = null
)
