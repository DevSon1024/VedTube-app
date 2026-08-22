package com.devson.vedtube.domain.model

/**
 * Domain model representing a video comment.
 */
data class Comment(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val commentText: String,
    val likeCount: Long = 0L,
    val publishDate: String? = null,
    val replyCount: Int = 0
)
