package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

/**
 * Room Entity representing a downloaded video or an active video download job.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
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
)
