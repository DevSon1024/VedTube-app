package com.devson.vedtube.core.database.model

import androidx.room.Entity

/**
 * Room Entity representing a locally subscribed channel, isolated per user profile.
 */
@Entity(
    tableName = "subscriptions",
    primaryKeys = ["profileId", "channelId"]
)
data class SubscriptionEntity(
    val channelId: String,
    val profileId: String = "profile_default",
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)
