package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devson.vedtube.core.database.model.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(entity: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQueries(profileId: String = "profile_default", limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getAllQueriesSync(profileId: String = "profile_default"): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllQueriesAllProfilesSync(): List<SearchHistoryEntity>

    @Query("DELETE FROM search_history WHERE `query` = :query AND profileId = :profileId")
    suspend fun deleteQuery(query: String, profileId: String = "profile_default")

    @Query("DELETE FROM search_history WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String = "profile_default")

    @Query("DELETE FROM search_history")
    suspend fun clearAllProfiles()
}
