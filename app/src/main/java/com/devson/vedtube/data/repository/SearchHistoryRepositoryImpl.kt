package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.SearchHistoryDao
import com.devson.vedtube.core.database.model.SearchHistoryEntity
import com.devson.vedtube.core.datastore.UserPreferencesDataStore
import com.devson.vedtube.domain.model.SearchHistoryItem
import com.devson.vedtube.domain.repository.SearchHistoryRepository
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
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SearchHistoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRecentQueries(limit: Int): Flow<List<SearchHistoryItem>> {
        return userPreferencesDataStore.activeProfileId
            .flatMapLatest { profileId ->
                searchHistoryDao.getRecentQueries(profileId, limit)
            }
            .map { list ->
                list.map { entity ->
                    SearchHistoryItem(
                        query = entity.query,
                        timestamp = entity.timestamp
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        withContext(ioDispatcher) {
            val profileId = userPreferencesDataStore.activeProfileId.first()
            searchHistoryDao.insertQuery(
                SearchHistoryEntity(
                    query = trimmed,
                    profileId = profileId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteQuery(query: String) {
        withContext(ioDispatcher) {
            val profileId = userPreferencesDataStore.activeProfileId.first()
            searchHistoryDao.deleteQuery(query, profileId)
        }
    }

    override suspend fun clearHistory() {
        withContext(ioDispatcher) {
            val profileId = userPreferencesDataStore.activeProfileId.first()
            searchHistoryDao.clearAll(profileId)
        }
    }
}
