package com.devson.vedtube.domain.model

enum class VideoQuality(val maxResolution: Int) {
    AUTO(Int.MAX_VALUE),
    UHD_4K(2160),
    QHD_1440(1440),
    HD_1080(1080),
    HD_720(720),
    SD_480(480),
    SD_360(360),
    LOW_144(144)
}

/**
 * User and app preferences for resolving and selecting media stream variants.
 */
data class PlaybackPreferences(
    val preferredQuality: VideoQuality = VideoQuality.AUTO,
    val preferAdaptiveStreams: Boolean = true,
    val preferHlsOrDash: Boolean = false
)
