package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.DownloadItem
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    /**
     * Observe all downloads sorted by creation date descending.
     */
    fun getAllDownloads(): Flow<List<DownloadItem>>

    /**
     * Observe the download state of a specific video.
     */
    fun getDownload(videoId: String): Flow<DownloadItem?>

    /**
     * Synchronously/directly get the download state of a specific video.
     */
    suspend fun getDownloadSync(videoId: String): DownloadItem?

    /**
     * Enqueue a new video download with the selected video stream quality.
     */
    suspend fun startDownload(video: Video, stream: VideoStream)

    /**
     * Cancel an active download.
     */
    suspend fun cancelDownload(videoId: String)

    /**
     * Delete a downloaded video entry from database and remove its local file from disk.
     */
    suspend fun deleteDownload(videoId: String)
}
