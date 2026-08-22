package com.devson.vedtube.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.devson.vedtube.R
import com.devson.vedtube.core.database.dao.DownloadDao
import com.devson.vedtube.core.database.model.DownloadStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Robust background worker that downloads video streams using OkHttp,
 * updates progress in Room, and stores the completed MP4 in app-specific storage.
 */
class VideoDownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun downloadDao(): DownloadDao
        fun okHttpClient(): OkHttpClient
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
    }

    private val downloadDao by lazy { entryPoint.downloadDao() }
    private val okHttpClient by lazy { entryPoint.okHttpClient() }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Video"
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val targetFilePath = inputData.getString(KEY_TARGET_FILE_PATH) ?: return@withContext Result.failure()

        val targetFile = File(targetFilePath)
        val tempFile = File("${targetFilePath}.tmp")

        // Ensure parent directories exist
        targetFile.parentFile?.mkdirs()

        val notificationId = videoId.hashCode()
        createNotificationChannel()

        try {
            // Promote to foreground service so the OS does not kill it when the app is minimized
            val initialForegroundInfo = createForegroundInfo(notificationId, title, 0)
            setForeground(initialForegroundInfo)

            downloadDao.updateStatus(videoId, DownloadStatus.DOWNLOADING)

            val request = Request.Builder()
                .url(streamUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with HTTP ${response.code} for $videoId")
                downloadDao.updateStatus(videoId, DownloadStatus.FAILED)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                Log.e(TAG, "Response body was null for $videoId")
                downloadDao.updateStatus(videoId, DownloadStatus.FAILED)
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastEmittedProgress = 0
            var lastProgressUpdateTime = System.currentTimeMillis()

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(64 * 1024) // 64KB buffer
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        Log.w(TAG, "Download stopped/cancelled for $videoId")
                        tempFile.delete()
                        downloadDao.updateStatus(videoId, DownloadStatus.FAILED)
                        return@withContext Result.failure()
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progressPercent = if (totalBytes > 0L) {
                        ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    val now = System.currentTimeMillis()
                    // Update database every 2% progress or every 1000ms
                    if (progressPercent > lastEmittedProgress + 1 || (now - lastProgressUpdateTime > 1000L)) {
                        lastEmittedProgress = progressPercent
                        lastProgressUpdateTime = now

                        downloadDao.updateProgress(
                            videoId = videoId,
                            status = DownloadStatus.DOWNLOADING,
                            progress = progressPercent,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )

                        // Update Notification
                        try {
                            setForeground(createForegroundInfo(notificationId, title, progressPercent))
                        } catch (e: Exception) {
                            // Non-critical notification update catch
                        }
                    }
                }

                outputStream.flush()
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }

            // Rename .tmp to final .mp4 target file
            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                if (!renamed) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }

            // Mark completed in database
            downloadDao.updateProgress(
                videoId = videoId,
                status = DownloadStatus.COMPLETED,
                progress = 100,
                downloadedBytes = downloadedBytes,
                totalBytes = if (totalBytes > 0L) totalBytes else downloadedBytes
            )

            Log.i(TAG, "Download completed successfully: $videoId at $targetFilePath")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for $videoId: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            downloadDao.updateStatus(videoId, DownloadStatus.FAILED)
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active video downloads"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(
        notificationId: Int,
        title: String,
        progress: Int
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading video")
            .setContentText("$title ($progress%)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        private const val TAG = "VideoDownloadWorker"
        const val CHANNEL_ID = "vedtube_downloads_channel"

        const val KEY_VIDEO_ID = "key_video_id"
        const val KEY_TITLE = "key_title"
        const val KEY_CHANNEL_NAME = "key_channel_name"
        const val KEY_THUMBNAIL_URL = "key_thumbnail_url"
        const val KEY_STREAM_URL = "key_stream_url"
        const val KEY_QUALITY = "key_quality"
        const val KEY_TARGET_FILE_PATH = "key_target_file_path"
    }
}
