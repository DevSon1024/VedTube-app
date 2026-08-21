package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a video in the local watch history.
 */
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val progressMs: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
