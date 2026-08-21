package com.devson.vedtube.core.player.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.devson.vedtube.MainActivity
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

        @Provides
        @Singleton
        fun providesMediaSession(
            @ApplicationContext context: Context,
            exoPlayer: ExoPlayer
        ): MediaSession {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            return MediaSession.Builder(context, exoPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        }
    }
}
