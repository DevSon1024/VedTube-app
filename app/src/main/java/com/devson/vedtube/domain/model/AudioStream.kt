package com.devson.vedtube.domain.model

/**
 * Domain representation of an audio stream variant.
 */
data class AudioStream(
    val url: String,
    val bitrate: Int = 0,
    val averageBitrate: Int = 0,
    val codec: String? = null,
    val mimeType: String = "audio/mp4",
    val format: String? = null
)
