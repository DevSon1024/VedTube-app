package com.devson.vedtube.data.provider.youtube.cobalt

import com.devson.vedtube.data.provider.youtube.cobalt.model.CobaltResponse
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.VideoStream

object CobaltMapper {

    private const val DEFAULT_EXPIRY_DURATION_MS = 6 * 3600 * 1000L

    fun map(response: CobaltResponse, videoId: String): PlaybackSource {
        val streamUrl = response.url
            ?: response.picker.firstOrNull { !it.url.isNullOrBlank() }?.url
            ?: ""

        val videoStreams = if (streamUrl.isNotBlank()) {
            listOf(
                VideoStream(
                    url = streamUrl,
                    resolution = "720p",
                    width = 1280,
                    height = 720,
                    bitrate = 1_500_000,
                    fps = 30,
                    codec = "avc1",
                    mimeType = "video/mp4",
                    isVideoOnly = false,
                    format = "mp4"
                )
            )
        } else {
            emptyList()
        }

        return PlaybackSource(
            videoId = videoId,
            title = response.filename?.substringBeforeLast('.'),
            uploaderName = null,
            streams = videoStreams,
            audioStreams = emptyList(),
            subtitles = emptyList(),
            durationMs = null,
            expiresAtTimestampMs = System.currentTimeMillis() + DEFAULT_EXPIRY_DURATION_MS,
            dashManifestUrl = null,
            hlsManifestUrl = null
        )
    }
}
