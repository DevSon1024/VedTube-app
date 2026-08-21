package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.WatchHistoryDao
import com.devson.vedtube.core.database.model.WatchHistoryEntity
import com.devson.vedtube.domain.model.WatchHistoryItem
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {

    override fun getRecentHistory(): Flow<List<WatchHistoryItem>> {
        return watchHistoryDao.getRecentHistory()
            .map { list ->
                list.map { entity ->
                    WatchHistoryItem(
                        videoId = entity.videoId,
                        title = entity.title,
                        channelName = entity.channelName,
                        thumbnailUrl = entity.thumbnailUrl,
                        durationMs = entity.durationMs,
                        progressMs = entity.progressMs,
                        lastWatchedAt = entity.lastWatchedAt
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getProgress(videoId: String): Long? = withContext(ioDispatcher) {
        watchHistoryDao.getProgress(videoId)
    }

    override suspend fun saveProgress(
        videoId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        durationMs: Long,
        progressMs: Long
    ) = withContext(ioDispatcher) {
        val entity = WatchHistoryEntity(
            videoId = videoId,
            title = title,
            channelName = channelName,
            thumbnailUrl = thumbnailUrl,
            durationMs = durationMs,
            progressMs = progressMs,
            lastWatchedAt = System.currentTimeMillis()
        )
        watchHistoryDao.upsert(entity)
    }

    override suspend fun deleteHistory(videoId: String) = withContext(ioDispatcher) {
        watchHistoryDao.deleteHistory(videoId)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        watchHistoryDao.clearHistory()
    }
}
