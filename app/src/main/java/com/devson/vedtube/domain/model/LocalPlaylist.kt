package com.devson.vedtube.domain.model

/**
 * Domain model representing a user's custom playlist.
 */
data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val thumbnailUrl: String? = null
)

/**
 * Detailed playlist model containing its list of videos.
 */
data class LocalPlaylistDetail(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val videos: List<Video> = emptyList()
)
