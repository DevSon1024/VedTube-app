package com.devson.vedtube.domain.model

/**
 * Domain representation of a watch history record.
 */
data class WatchHistoryItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val progressMs: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
