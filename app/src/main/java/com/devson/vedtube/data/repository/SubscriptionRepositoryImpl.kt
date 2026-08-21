package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.SubscriptionDao
import com.devson.vedtube.core.database.model.SubscriptionEntity
import com.devson.vedtube.domain.model.ChannelSubscription
import com.devson.vedtube.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<ChannelSubscription>> {
        return subscriptionDao.getAllSubscriptions()
            .map { list ->
                list.map { entity ->
                    ChannelSubscription(
                        channelId = entity.channelId,
                        channelName = entity.channelName,
                        avatarUrl = entity.avatarUrl,
                        subscribedAt = entity.subscribedAt
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun isSubscribed(channelId: String): Flow<Boolean> {
        return subscriptionDao.isSubscribed(channelId).flowOn(ioDispatcher)
    }

    override suspend fun isSubscribedSync(channelId: String): Boolean = withContext(ioDispatcher) {
        subscriptionDao.isSubscribedSync(channelId)
    }

    override suspend fun subscribe(channelId: String, channelName: String, avatarUrl: String) =
        withContext(ioDispatcher) {
            val entity = SubscriptionEntity(
                channelId = channelId,
                channelName = channelName,
                avatarUrl = avatarUrl,
                subscribedAt = System.currentTimeMillis()
            )
            subscriptionDao.insert(entity)
        }

    override suspend fun unsubscribe(channelId: String) = withContext(ioDispatcher) {
        subscriptionDao.delete(channelId)
    }
}
