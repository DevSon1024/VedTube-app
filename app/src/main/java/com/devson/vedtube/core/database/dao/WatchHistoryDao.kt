package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devson.vedtube.core.database.model.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Upsert
    suspend fun upsert(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getRecentHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getHistory(videoId: String): WatchHistoryEntity?

    @Query("SELECT progressMs FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getProgress(videoId: String): Long?

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteHistory(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}
