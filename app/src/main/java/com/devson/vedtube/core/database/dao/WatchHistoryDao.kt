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

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    fun getRecentHistory(profileId: String = "profile_default"): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    suspend fun getAllHistorySync(profileId: String = "profile_default"): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    suspend fun getAllHistoryAllProfilesSync(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId AND profileId = :profileId LIMIT 1")
    suspend fun getHistory(videoId: String, profileId: String = "profile_default"): WatchHistoryEntity?

    @Query("SELECT progressMs FROM watch_history WHERE videoId = :videoId AND profileId = :profileId LIMIT 1")
    suspend fun getProgress(videoId: String, profileId: String = "profile_default"): Long?

    @Query("DELETE FROM watch_history WHERE videoId = :videoId AND profileId = :profileId")
    suspend fun deleteHistory(videoId: String, profileId: String = "profile_default")

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String = "profile_default")

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
