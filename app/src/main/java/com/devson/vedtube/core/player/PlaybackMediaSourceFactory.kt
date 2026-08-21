package com.devson.vedtube.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates Media3 [MediaSource] instances from domain [PlaybackSource] models.
 * Handles progressive streams (.mp4), adaptive video+audio merging, DASH (.mpd), and HLS (.m3u8).
 */
@Singleton
@OptIn(UnstableApi::class)
class PlaybackMediaSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val okHttpDataSourceFactory: DataSource.Factory by lazy {
        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.youtube.com/",
                    "Origin" to "https://www.youtube.com"
                )
            )
        DefaultDataSource.Factory(context, okHttpFactory)
    }

    fun createMediaSource(
        playbackSource: PlaybackSource,
        preferences: PlaybackPreferences = PlaybackPreferences()
    ): MediaSource {
        // 1. Select best video and audio stream variants based on preferences
        val selectedVideoStream = playbackSource.selectBestStream(preferences)
        val selectedAudioStream = playbackSource.selectBestAudioStream()

        // 2. If separate video-only stream and audio stream -> combine via MergingMediaSource
        if (selectedVideoStream != null && selectedVideoStream.isVideoOnly && selectedAudioStream != null) {
            val videoMediaSource = ProgressiveMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(selectedVideoStream.url))
            val audioMediaSource = ProgressiveMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(selectedAudioStream.url))

            return MergingMediaSource(videoMediaSource, audioMediaSource)
        }

        // 3. Resolve primary playable URL (progressive .mp4, HLS .m3u8, or DASH .mpd)
        val primaryStreamUrl = selectedVideoStream?.url
            ?: playbackSource.hlsManifestUrl
            ?: selectedAudioStream?.url
            ?: playbackSource.dashManifestUrl
            ?: error("No playable stream URL found in PlaybackSource")

        val mediaItemBuilder = MediaItem.Builder().setUri(primaryStreamUrl)

        if (playbackSource.subtitles.isNotEmpty()) {
            val subConfigs = playbackSource.subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(sub.mimeType.ifBlank { MimeTypes.TEXT_VTT })
                    .setLanguage(sub.languageCode)
                    .setLabel(sub.languageName)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subConfigs)
        }

        val mediaItem = mediaItemBuilder.build()

        return when {
            primaryStreamUrl.contains(".m3u8") || primaryStreamUrl.contains("hls") -> {
                HlsMediaSource.Factory(okHttpDataSourceFactory).createMediaSource(mediaItem)
            }
            primaryStreamUrl.contains(".mpd") || primaryStreamUrl.contains("dash") -> {
                DashMediaSource.Factory(okHttpDataSourceFactory).createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(okHttpDataSourceFactory).createMediaSource(mediaItem)
            }
        }
    }
}
