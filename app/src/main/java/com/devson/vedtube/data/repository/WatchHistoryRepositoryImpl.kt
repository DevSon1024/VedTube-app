package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.WatchHistoryDao
import com.devson.vedtube.core.database.model.WatchHistoryEntity
import com.devson.vedtube.core.datastore.UserPreferencesDataStore
import com.devson.vedtube.domain.model.WatchHistoryItem
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRecentHistory(): Flow<List<WatchHistoryItem>> {
        return userPreferencesDataStore.activeProfileId
            .flatMapLatest { profileId ->
                watchHistoryDao.getRecentHistory(profileId)
            }
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
        val profileId = userPreferencesDataStore.activeProfileId.first()
        watchHistoryDao.getProgress(videoId, profileId)
    }

    override suspend fun saveProgress(
        videoId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        durationMs: Long,
        progressMs: Long
    ) = withContext(ioDispatcher) {
        val profileId = userPreferencesDataStore.activeProfileId.first()
        val entity = WatchHistoryEntity(
            videoId = videoId,
            profileId = profileId,
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
        val profileId = userPreferencesDataStore.activeProfileId.first()
        watchHistoryDao.deleteHistory(videoId, profileId)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        val profileId = userPreferencesDataStore.activeProfileId.first()
        watchHistoryDao.clearHistory(profileId)
    }
}
