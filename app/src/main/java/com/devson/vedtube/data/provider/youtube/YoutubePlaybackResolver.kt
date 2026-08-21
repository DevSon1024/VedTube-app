package com.devson.vedtube.data.provider.youtube

import android.util.Log
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.data.provider.youtube.cobalt.CobaltApiService
import com.devson.vedtube.data.provider.youtube.cobalt.CobaltMapper
import com.devson.vedtube.data.provider.youtube.cobalt.model.CobaltRequest
import com.devson.vedtube.data.provider.youtube.cobalt.model.CobaltResponse
import com.devson.vedtube.data.provider.youtube.invidious.InvidiousMapper
import com.devson.vedtube.data.provider.youtube.invidious.model.InvidiousStreamResponse
import com.devson.vedtube.data.provider.youtube.piped.PipedApiService
import com.devson.vedtube.data.provider.youtube.piped.PipedMapper
import com.devson.vedtube.data.provider.youtube.piped.model.PipedStreamResponse
import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.resolver.PlaybackResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [PlaybackResolver] with multi-tier resilience:
 * 1. Primary: Native [NewPipeExtractorDataSource] direct stream extraction (master-SNAPSHOT).
 * 2. Primary Fallback: Cobalt API (rarely DNS blocked, returns direct MP4 stream).
 * 3. Secondary Fallback: Rotating public Invidious API instances.
 * 4. Tertiary Fallback: Rotating public Piped API instances.
 */
@Singleton
class YoutubePlaybackResolver @Inject constructor(
    private val extractorDataSource: YoutubeExtractorDataSource,
    private val cobaltApiService: CobaltApiService,
    private val pipedApiService: PipedApiService,
    private val json: Json,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : PlaybackResolver {

    val cobaltInstances: List<String> = listOf(
        "https://co.wuk.sh",
        "https://cobalt-api.kwiatekm.pl",
        "https://api.cobalt.tools",
        "https://cobalt.api.screc.top"
    )

    val invidiousInstances: List<String> = listOf(
        "https://iv.ggtyler.dev",
        "https://invidious.nerdvpn.de",
        "https://inv.nadeko.net",
        "https://invidious.jing.rocks"
    )

    val pipedInstances: List<String> = listOf(
        "https://pipedapi.adminforge.de",
        "https://pipedapi.moomoo.me",
        "https://pipedapi.astartes.nl",
        "https://api.piped.privacy.com.de",
        "https://pipedapi.kavin.rocks"
    )

    override suspend fun resolve(
        videoId: String,
        preferences: PlaybackPreferences
    ): Result<PlaybackSource> = withContext(ioDispatcher) {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"

        // 1. Primary: Native NewPipeExtractor
        try {
            val streamInfo = extractorDataSource.extractStreamInfo(videoId)
            val playbackSource = YoutubePlaybackMapper.map(streamInfo, videoId)
            if (hasPlayableContent(playbackSource)) {
                return@withContext Result.success(playbackSource)
            }
        } catch (e: Throwable) {
            Log.w("VedTube", "NewPipe direct extractor failed for video: $videoId. Trying Cobalt fallback...", e)
        }

        // 2. Primary Fallback: Cobalt API (POST /api/json)
        val cobaltRequest = CobaltRequest(url = watchUrl)
        for (instance in cobaltInstances) {
            try {
                val cleanBase = instance.trimEnd('/')
                val fullUrl = "$cleanBase/api/json"
                val response = cobaltApiService.resolveStream(fullUrl, cobaltRequest)

                if (response.isSuccessful && response.body() != null) {
                    val responseBodyString = response.body()!!.string()
                    val cobaltResponse = json.decodeFromString<CobaltResponse>(responseBodyString)
                    val playbackSource = CobaltMapper.map(cobaltResponse, videoId)

                    if (hasPlayableContent(playbackSource)) {
                        // RETURN IMMEDIATELY. DO NOT CONTINUE THE LOOP!
                        return@withContext Result.success(playbackSource)
                    }
                }
            } catch (e: Exception) {
                Log.e("VedTube", "Failed to parse Cobalt stream from $instance: ${e.message}", e)
                continue
            }
        }

        // 3. Secondary Fallback: Rotating Invidious API instances
        for (instance in invidiousInstances) {
            try {
                val cleanBase = instance.trimEnd('/')
                val fullUrl = "$cleanBase/api/v1/videos/$videoId"
                val response = pipedApiService.getStreamInfoFromUrl(fullUrl)

                if (response.isSuccessful && response.body() != null) {
                    val responseBodyString = response.body()!!.string()
                    val invidiousResponse = json.decodeFromString<InvidiousStreamResponse>(responseBodyString)
                    val playbackSource = InvidiousMapper.map(invidiousResponse, videoId)

                    if (hasPlayableContent(playbackSource)) {
                        // RETURN IMMEDIATELY. DO NOT CONTINUE THE LOOP!
                        return@withContext Result.success(playbackSource)
                    }
                }
            } catch (e: Exception) {
                Log.e("VedTube", "Failed to parse Invidious stream from $instance: ${e.message}", e)
                continue
            }
        }

        // 4. Tertiary Fallback: Rotating Piped API instances
        for (instance in pipedInstances) {
            try {
                val cleanBase = instance.trimEnd('/')
                val fullUrl = "$cleanBase/streams/$videoId"
                val response = pipedApiService.getStreamInfoFromUrl(fullUrl)

                if (response.isSuccessful && response.body() != null) {
                    val responseBodyString = response.body()!!.string()
                    val pipedResponse = json.decodeFromString<PipedStreamResponse>(responseBodyString)
                    val playbackSource = PipedMapper.map(pipedResponse, videoId)

                    if (hasPlayableContent(playbackSource)) {
                        // RETURN IMMEDIATELY. DO NOT CONTINUE THE LOOP!
                        return@withContext Result.success(playbackSource)
                    }
                }
            } catch (e: Exception) {
                Log.e("VedTube", "Failed to parse Piped stream from $instance: ${e.message}", e)
                continue
            }
        }

        Result.failure(
            AppError.ContentUnavailable("All extraction providers (NewPipe, Cobalt, Invidious, Piped) failed for video: $videoId")
        )
    }

    private fun hasPlayableContent(source: PlaybackSource): Boolean {
        return source.streams.isNotEmpty() ||
                source.audioStreams.isNotEmpty() ||
                !source.hlsManifestUrl.isNullOrBlank() ||
                !source.dashManifestUrl.isNullOrBlank()
    }
}
