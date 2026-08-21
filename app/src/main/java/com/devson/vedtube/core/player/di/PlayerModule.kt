package com.devson.vedtube.core.player.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtube.core.player.PlayerController
import com.devson.vedtube.core.player.VedPlayer
import com.devson.vedtube.core.player.VedPlayerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindVedPlayer(impl: VedPlayerImpl): VedPlayer

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: VedPlayerImpl): PlayerController

    companion object {
        @Provides
        @Singleton
        fun providesExoPlayer(
            @ApplicationContext context: Context
        ): ExoPlayer {
            return ExoPlayer.Builder(context)
                .setHandleAudioBecomingNoisy(true)
                .build()
        }
    }
}
