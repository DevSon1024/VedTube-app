package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.SubscriptionDao
import com.devson.vedtube.core.database.model.SubscriptionEntity
import com.devson.vedtube.core.datastore.UserPreferencesDataStore
import com.devson.vedtube.domain.model.ChannelSubscription
import com.devson.vedtube.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllSubscriptions(): Flow<List<ChannelSubscription>> {
        return userPreferencesDataStore.activeProfileId
            .flatMapLatest { profileId ->
                subscriptionDao.getAllSubscriptions(profileId)
            }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isSubscribed(channelId: String): Flow<Boolean> {
        return userPreferencesDataStore.activeProfileId
            .flatMapLatest { profileId ->
                subscriptionDao.isSubscribed(channelId, profileId)
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun isSubscribedSync(channelId: String): Boolean = withContext(ioDispatcher) {
        val profileId = userPreferencesDataStore.activeProfileId.first()
        subscriptionDao.isSubscribedSync(channelId, profileId)
    }

    override suspend fun subscribe(channelId: String, channelName: String, avatarUrl: String) =
        withContext(ioDispatcher) {
            val profileId = userPreferencesDataStore.activeProfileId.first()
            val entity = SubscriptionEntity(
                channelId = channelId,
                profileId = profileId,
                channelName = channelName,
                avatarUrl = avatarUrl,
                subscribedAt = System.currentTimeMillis()
            )
            subscriptionDao.insert(entity)
        }

    override suspend fun unsubscribe(channelId: String) = withContext(ioDispatcher) {
        val profileId = userPreferencesDataStore.activeProfileId.first()
        subscriptionDao.delete(channelId, profileId)
    }
}
