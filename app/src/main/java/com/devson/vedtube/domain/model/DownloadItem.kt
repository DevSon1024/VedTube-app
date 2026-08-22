package com.devson.vedtube.domain.model

import com.devson.vedtube.core.database.model.DownloadStatus

/**
 * Domain model representing a video download item with reactive status and progress.
 */
data class DownloadItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val localFilePath: String,
    val streamUrl: String,
    val quality: String,
    val status: DownloadStatus,
    val progress: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = when {
            progress > 0 -> (progress / 100f).coerceIn(0f, 1f)
            totalBytes > 0L -> (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED

    val isDownloading: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING

    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED
}
