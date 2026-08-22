package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a recorded search query.
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
