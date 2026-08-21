package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a locally subscribed channel.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey
    val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)
