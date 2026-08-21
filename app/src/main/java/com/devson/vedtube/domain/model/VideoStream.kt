package com.devson.vedtube.domain.model

/**
 * Domain representation of a video stream variant.
 */
data class VideoStream(
    val url: String,
    val resolution: String,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Int = 0,
    val fps: Int = 30,
    val codec: String? = null,
    val mimeType: String = "video/mp4",
    val isVideoOnly: Boolean = false,
    val format: String? = null
)
