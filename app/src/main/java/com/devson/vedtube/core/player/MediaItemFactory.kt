package com.devson.vedtube.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.devson.vedtube.domain.model.VideoStream
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory responsible for creating Media3 [MediaSource] and [MediaItem] objects
 * from domain [PlaybackSource] and [VideoStream] models.
 */
@Singleton
@OptIn(UnstableApi::class)
class MediaItemFactory @Inject constructor(
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

    /**
     * Builds a [MediaSource] for the given [PlaybackSource], using either a specific [overrideQuality]
     * or selecting the best stream matching [preferences].
     */
    fun createMediaSource(
        playbackSource: PlaybackSource,
        overrideQuality: VideoStream? = null,
        preferences: PlaybackPreferences = PlaybackPreferences()
    ): MediaSource {
        val selectedVideoStream = overrideQuality ?: playbackSource.selectBestStream(preferences)
        val selectedAudioStream = playbackSource.selectBestAudioStream()

        // 1. Separate video-only + audio stream -> MergingMediaSource
        if (selectedVideoStream != null && selectedVideoStream.isVideoOnly && selectedAudioStream != null) {
            val videoMediaSource = ProgressiveMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(buildMediaItem(selectedVideoStream.url, playbackSource))
            val audioMediaSource = ProgressiveMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(buildMediaItem(selectedAudioStream.url, playbackSource))

            return MergingMediaSource(videoMediaSource, audioMediaSource)
        }

        // 2. Progressive or adaptive manifest URL
        val primaryStreamUrl = selectedVideoStream?.url
            ?: playbackSource.hlsManifestUrl
            ?: selectedAudioStream?.url
            ?: playbackSource.dashManifestUrl
            ?: error("No playable stream URL found in PlaybackSource")

        val mediaItem = buildMediaItem(primaryStreamUrl, playbackSource)

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

    private fun buildMediaItem(streamUrl: String, playbackSource: PlaybackSource): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(playbackSource.title)
            .setArtist(playbackSource.uploaderName)
            .build()

        val builder = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(playbackSource.videoId)
            .setMediaMetadata(metadata)

        if (playbackSource.subtitles.isNotEmpty()) {
            val subConfigs = playbackSource.subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(sub.mimeType.ifBlank { MimeTypes.TEXT_VTT })
                    .setLanguage(sub.languageCode)
                    .setLabel(sub.languageName)
                    .build()
            }
            builder.setSubtitleConfigurations(subConfigs)
        }

        return builder.build()
    }
}
