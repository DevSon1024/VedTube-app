package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devson.vedtube.core.database.model.DownloadEntity
import com.devson.vedtube.core.database.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    fun getDownload(videoId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadSync(videoId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("UPDATE downloads SET status = :status, progress = :progress, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE videoId = :videoId")
    suspend fun updateProgress(
        videoId: String,
        status: DownloadStatus,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    )

    @Query("UPDATE downloads SET status = :status WHERE videoId = :videoId")
    suspend fun updateStatus(videoId: String, status: DownloadStatus)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
