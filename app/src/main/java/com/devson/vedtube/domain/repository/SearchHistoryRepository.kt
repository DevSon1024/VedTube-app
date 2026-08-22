package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.SearchHistoryItem
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getRecentQueries(limit: Int = 20): Flow<List<SearchHistoryItem>>
    suspend fun saveQuery(query: String)
    suspend fun deleteQuery(query: String)
    suspend fun clearHistory()
}
