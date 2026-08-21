package com.devson.vedtube.domain.model

/**
 * Decoupled, provider-independent model containing all resolved stream assets for playback.
 */
data class PlaybackSource(
    val videoId: String,
    val title: String? = null,
    val uploaderName: String? = null,
    val thumbnailUrl: String? = null,
    val streams: List<VideoStream> = emptyList(),
    val audioStreams: List<AudioStream> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val durationMs: Long? = null,
    val expiresAtTimestampMs: Long? = null,
    val dashManifestUrl: String? = null,
    val hlsManifestUrl: String? = null
) {
    val isExpired: Boolean
        get() = expiresAtTimestampMs != null && System.currentTimeMillis() >= expiresAtTimestampMs

    /**
     * Selects the best video stream matching the user's preferred resolution.
     */
    fun selectBestStream(preferences: PlaybackPreferences = PlaybackPreferences()): VideoStream? {
        val targetMaxHeight = preferences.preferredQuality.maxResolution

        // 1. Check progressive streams (combined video + audio)
        val progressiveStreams = streams.filter { !it.isVideoOnly }
        val matchingProgressive = progressiveStreams.filter { it.height <= targetMaxHeight }

        if (matchingProgressive.isNotEmpty()) {
            return matchingProgressive.maxByOrNull { it.height }
        } else if (progressiveStreams.isNotEmpty()) {
            return progressiveStreams.minByOrNull { it.height }
        }

        // 2. If no progressive stream found, check all streams (video-only / adaptive)
        val matchingAll = streams.filter { it.height <= targetMaxHeight }
        return matchingAll.maxByOrNull { it.height } ?: streams.maxByOrNull { it.height }
    }

    /**
     * Selects the highest quality audio stream available.
     */
    fun selectBestAudioStream(): AudioStream? {
        return audioStreams.maxByOrNull { it.bitrate }
    }
}
