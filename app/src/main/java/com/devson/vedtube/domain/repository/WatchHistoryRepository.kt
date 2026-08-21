package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getRecentHistory(): Flow<List<WatchHistoryItem>>
    suspend fun getProgress(videoId: String): Long?
    suspend fun saveProgress(
        videoId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        durationMs: Long,
        progressMs: Long
    )
    suspend fun deleteHistory(videoId: String)
    suspend fun clearHistory()
}
