package com.devson.vedtube.data.di

import com.devson.vedtube.data.provider.youtube.NewPipeExtractorDataSource
import com.devson.vedtube.data.provider.youtube.YoutubeExtractorDataSource
import com.devson.vedtube.data.provider.youtube.YoutubeProviderImpl
import com.devson.vedtube.domain.provider.MediaProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @Singleton
    abstract fun bindMediaProvider(
        youtubeProviderImpl: YoutubeProviderImpl
    ): MediaProvider

    @Binds
    @Singleton
    abstract fun bindYoutubeExtractorDataSource(
        newPipeExtractorDataSource: NewPipeExtractorDataSource
    ): YoutubeExtractorDataSource

    @Binds
    @Singleton
    abstract fun bindPlaybackResolver(
        youtubePlaybackResolver: com.devson.vedtube.data.provider.youtube.YoutubePlaybackResolver
    ): com.devson.vedtube.domain.resolver.PlaybackResolver
}
