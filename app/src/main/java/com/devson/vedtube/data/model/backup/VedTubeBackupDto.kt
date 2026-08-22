package com.devson.vedtube.data.model.backup

import kotlinx.serialization.Serializable

@Serializable
data class VedTubeBackupDto(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val subscriptions: List<SubscriptionBackupDto> = emptyList(),
    val watchHistory: List<WatchHistoryBackupDto> = emptyList(),
    val playlists: List<PlaylistBackupDto> = emptyList(),
    val searchHistory: List<SearchHistoryBackupDto> = emptyList()
)

@Serializable
data class SubscriptionBackupDto(
    val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = 0L
)

@Serializable
data class WatchHistoryBackupDto(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val progressMs: Long = 0L,
    val lastWatchedAt: Long = 0L
)

@Serializable
data class PlaylistBackupDto(
    val playlistId: String,
    val name: String,
    val createdAt: Long = 0L,
    val videos: List<PlaylistVideoBackupDto> = emptyList()
)

@Serializable
data class PlaylistVideoBackupDto(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Long = 0L,
    val addedAt: Long = 0L
)

@Serializable
data class SearchHistoryBackupDto(
    val query: String,
    val timestamp: Long = 0L
)
