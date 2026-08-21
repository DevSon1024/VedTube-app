package com.devson.vedtube.domain.model

/**
 * Domain model representing a YouTube playlist.
 */
data class Playlist(
    val id: String,
    val title: String,
    val uploaderName: String? = null,
    val thumbnailUrl: String? = null,
    val videoCount: Long = 0,
    val videos: List<Video> = emptyList()
)
