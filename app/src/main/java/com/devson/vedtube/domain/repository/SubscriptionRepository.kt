package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.ChannelSubscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getAllSubscriptions(): Flow<List<ChannelSubscription>>
    fun isSubscribed(channelId: String): Flow<Boolean>
    suspend fun isSubscribedSync(channelId: String): Boolean
    suspend fun subscribe(channelId: String, channelName: String, avatarUrl: String)
    suspend fun unsubscribe(channelId: String)
}
