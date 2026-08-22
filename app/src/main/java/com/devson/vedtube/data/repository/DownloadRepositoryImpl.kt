package com.devson.vedtube.data.repository

import android.content.Context
import android.os.Environment
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.DownloadDao
import com.devson.vedtube.core.database.model.DownloadEntity
import com.devson.vedtube.core.database.model.DownloadStatus
import com.devson.vedtube.core.download.VideoDownloadWorker
import com.devson.vedtube.domain.model.DownloadItem
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoStream
import com.devson.vedtube.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DownloadRepository {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { list ->
            list.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    override fun getDownload(videoId: String): Flow<DownloadItem?> {
        return downloadDao.getDownload(videoId).map { it?.toDomain() }.flowOn(ioDispatcher)
    }

    override suspend fun getDownloadSync(videoId: String): DownloadItem? = withContext(ioDispatcher) {
        downloadDao.getDownloadSync(videoId)?.toDomain()
    }

    override suspend fun startDownload(video: Video, stream: VideoStream): Unit = withContext(ioDispatcher) {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "movies").apply { mkdirs() }
        
        // Clean video ID to avoid file system issues
        val sanitizedId = video.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val sanitizedQuality = stream.resolution.replace(Regex("[^a-zA-Z0-9]"), "")
        val targetFile = File(moviesDir, "${sanitizedId}_${sanitizedQuality}.mp4")

        val entity = DownloadEntity(
            videoId = video.id,
            title = video.title,
            channelName = video.uploaderName,
            thumbnailUrl = video.thumbnailUrl ?: "",
            localFilePath = targetFile.absolutePath,
            streamUrl = stream.url,
            quality = stream.resolution,
            status = DownloadStatus.PENDING,
            progress = 0,
            totalBytes = 0L,
            downloadedBytes = 0L,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.upsert(entity)

        val inputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_VIDEO_ID, video.id)
            .putString(VideoDownloadWorker.KEY_TITLE, video.title)
            .putString(VideoDownloadWorker.KEY_CHANNEL_NAME, video.uploaderName)
            .putString(VideoDownloadWorker.KEY_THUMBNAIL_URL, video.thumbnailUrl ?: "")
            .putString(VideoDownloadWorker.KEY_STREAM_URL, stream.url)
            .putString(VideoDownloadWorker.KEY_QUALITY, stream.resolution)
            .putString(VideoDownloadWorker.KEY_TARGET_FILE_PATH, targetFile.absolutePath)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("video_download")
            .addTag(video.id)
            .build()

        workManager.enqueueUniqueWork(
            "download_${video.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun cancelDownload(videoId: String): Unit = withContext(ioDispatcher) {
        workManager.cancelUniqueWork("download_$videoId")
        val entity = downloadDao.getDownloadSync(videoId)
        if (entity != null && entity.status != DownloadStatus.COMPLETED) {
            val file = File(entity.localFilePath)
            if (file.exists()) file.delete()
            val tempFile = File("${entity.localFilePath}.tmp")
            if (tempFile.exists()) tempFile.delete()
        }
        downloadDao.delete(videoId)
    }

    override suspend fun deleteDownload(videoId: String): Unit = withContext(ioDispatcher) {
        workManager.cancelUniqueWork("download_$videoId")
        val entity = downloadDao.getDownloadSync(videoId)
        if (entity != null) {
            val file = File(entity.localFilePath)
            if (file.exists()) file.delete()
            val tempFile = File("${entity.localFilePath}.tmp")
            if (tempFile.exists()) tempFile.delete()
        }
        downloadDao.delete(videoId)
    }

    private fun DownloadEntity.toDomain(): DownloadItem {
        return DownloadItem(
            videoId = videoId,
            title = title,
            channelName = channelName,
            thumbnailUrl = thumbnailUrl,
            localFilePath = localFilePath,
            streamUrl = streamUrl,
            quality = quality,
            status = status,
            progress = progress,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            createdAt = createdAt
        )
    }
}
