package com.devson.vedtube.domain.model

/**
 * Domain representation of a locally subscribed channel.
 */
data class ChannelSubscription(
    val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)
