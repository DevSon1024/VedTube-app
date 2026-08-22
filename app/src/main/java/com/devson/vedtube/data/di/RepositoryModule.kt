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

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: com.devson.vedtube.data.repository.DownloadRepositoryImpl
    ): com.devson.vedtube.domain.repository.DownloadRepository

    @Binds
    @Singleton
    abstract fun bindSponsorBlockRepository(
        sponsorBlockRepositoryImpl: com.devson.vedtube.data.repository.SponsorBlockRepositoryImpl
    ): com.devson.vedtube.domain.repository.SponsorBlockRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        searchHistoryRepositoryImpl: com.devson.vedtube.data.repository.SearchHistoryRepositoryImpl
    ): com.devson.vedtube.domain.repository.SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: com.devson.vedtube.data.repository.PlaylistRepositoryImpl
    ): com.devson.vedtube.domain.repository.PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindDataManagementRepository(
        dataManagementRepositoryImpl: com.devson.vedtube.data.repository.DataManagementRepositoryImpl
    ): com.devson.vedtube.domain.repository.DataManagementRepository
}
