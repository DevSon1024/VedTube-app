package com.devson.vedtube.data.di

import com.devson.vedtube.data.repository.SettingsRepositoryImpl
import com.devson.vedtube.data.repository.SubscriptionRepositoryImpl
import com.devson.vedtube.data.repository.WatchHistoryRepositoryImpl
import com.devson.vedtube.domain.repository.SettingsRepository
import com.devson.vedtube.domain.repository.SubscriptionRepository
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(
        watchHistoryRepositoryImpl: WatchHistoryRepositoryImpl
    ): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        subscriptionRepositoryImpl: SubscriptionRepositoryImpl
    ): SubscriptionRepository
}
