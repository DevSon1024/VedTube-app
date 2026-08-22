package com.devson.vedtube.core.database.model

import androidx.room.Entity

/**
 * Room Entity representing a recorded search query, isolated per user profile.
 */
@Entity(
    tableName = "search_history",
    primaryKeys = ["profileId", "query"]
)
data class SearchHistoryEntity(
    val query: String,
    val profileId: String = "profile_default",
    val timestamp: Long = System.currentTimeMillis()
)
