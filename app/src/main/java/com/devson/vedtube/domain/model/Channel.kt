package com.devson.vedtube.domain.model

/**
 * Domain model representing a YouTube channel.
 */
data class Channel(
    val id: String,
    val name: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val subscriberCount: Long? = null,
    val description: String? = null
)
