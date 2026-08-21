package com.devson.vedtube.domain.model

/**
 * Domain model representing a standard video item.
 */
data class Video(
    val id: String,
    val title: String,
    val uploaderName: String,
    val uploaderId: String? = null,
    val uploaderAvatarUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0,
    val viewCount: Long = 0,
    val uploadDate: String? = null
)
