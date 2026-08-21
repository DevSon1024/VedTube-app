package com.devson.vedtube.data.provider.youtube

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.data.provider.youtube.piped.PipedApiService
import com.devson.vedtube.data.provider.youtube.piped.PipedMapper
import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.resolver.PlaybackResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [PlaybackResolver] with dual-engine resilience:
 * 1. Primary: Native [NewPipeExtractorDataSource] direct stream extraction.
 * 2. Fallback: Rotating public Piped API instances that immediately returns upon the first 2xx response.
 */
@Singleton
class YoutubePlaybackResolver @Inject constructor(
    private val extractorDataSource: YoutubeExtractorDataSource,
    private val pipedApiService: PipedApiService,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : PlaybackResolver {

    val instances: List<String> = listOf(
        "https://sh.itjust.works",
        "https://pipedapi.syncpundit.io",
        "https://piped-api.garudalinux.org",
        "https://api.piped.privacydev.net",
        "https://pipedapi.tokhmi.xyz",
        "https://piped-api.lunar.icu",
        "https://pipedapi.kavin.rocks"
    )

    override suspend fun resolve(
        videoId: String,
        preferences: PlaybackPreferences
    ): Result<PlaybackSource> = withContext(ioDispatcher) {
        // 1. Primary: Native NewPipeExtractor
        try {
            val streamInfo = extractorDataSource.extractStreamInfo(videoId)
            val playbackSource = YoutubePlaybackMapper.map(streamInfo, videoId)
            if (hasPlayableContent(playbackSource)) {
                return@withContext Result.success(playbackSource)
            }
        } catch (e: Throwable) {
            // Log/ignore and proceed immediately to Piped instance rotation fallback
        }

        // 2. Fallback: Rotating Piped instances
        for (instance in instances) {
            try {
                val cleanBase = instance.trimEnd('/')
                val fullUrl = "$cleanBase/streams/$videoId"
                val response = pipedApiService.getStreamInfoFromUrl(fullUrl)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // 1. Map the body to your PlaybackSource domain model
                    val playbackSource = PipedMapper.map(body, videoId)

                    // 2. RETURN IMMEDIATELY. DO NOT CONTINUE THE LOOP!
                    return@withContext Result.success(playbackSource)
                }
            } catch (e: Exception) {
                // Ignore the exception and let the loop try the next instance
                continue
            }
        }

        Result.failure(
            AppError.ContentUnavailable("All Piped instances failed to resolve the stream for video: $videoId")
        )
    }

    private fun hasPlayableContent(source: PlaybackSource): Boolean {
        return source.streams.isNotEmpty() ||
                source.audioStreams.isNotEmpty() ||
                !source.hlsManifestUrl.isNullOrBlank() ||
                !source.dashManifestUrl.isNullOrBlank()
    }
}
