package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devson.vedtube.core.database.model.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId AND profileId = :profileId")
    suspend fun delete(channelId: String, profileId: String = "profile_default")

    @Query("SELECT * FROM subscriptions WHERE profileId = :profileId ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(profileId: String = "profile_default"): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE profileId = :profileId ORDER BY subscribedAt DESC")
    suspend fun getAllSubscriptionsSync(profileId: String = "profile_default"): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    suspend fun getAllSubscriptionsAllProfilesSync(): List<SubscriptionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId AND profileId = :profileId)")
    fun isSubscribed(channelId: String, profileId: String = "profile_default"): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId AND profileId = :profileId)")
    suspend fun isSubscribedSync(channelId: String, profileId: String = "profile_default"): Boolean

    @Query("DELETE FROM subscriptions WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String = "profile_default")

    @Query("DELETE FROM subscriptions")
    suspend fun clearAllProfiles()
}
