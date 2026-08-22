package com.devson.vedtube.core.database.model

import androidx.room.Entity

/**
 * Room Entity representing a video in the local watch history, isolated per user profile.
 */
@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "videoId"]
)
data class WatchHistoryEntity(
    val videoId: String,
    val profileId: String = "profile_default",
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val progressMs: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
