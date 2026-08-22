package com.devson.vedtube.data.repository

import android.util.Log
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.data.provider.sponsorblock.SponsorBlockApiService
import com.devson.vedtube.domain.model.SponsorSegment
import com.devson.vedtube.domain.repository.SponsorBlockRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SponsorBlockRepositoryImpl @Inject constructor(
    private val sponsorBlockApiService: SponsorBlockApiService,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SponsorBlockRepository {

    private val cache = ConcurrentHashMap<String, List<SponsorSegment>>()

    override suspend fun getSegments(videoId: String): List<SponsorSegment> = withContext(ioDispatcher) {
        if (videoId.isBlank()) return@withContext emptyList()

        cache[videoId]?.let { return@withContext it }

        try {
            val response = sponsorBlockApiService.getSkipSegments(videoId)
            if (response.isSuccessful) {
                val body = response.body().orEmpty()
                val segments = body.mapNotNull { item ->
                    if (item.segment.size >= 2) {
                        val start = (item.segment[0] * 1000.0).toLong()
                        val end = (item.segment[1] * 1000.0).toLong()
                        if (end > start) {
                            SponsorSegment(
                                category = item.category,
                                startMs = start,
                                endMs = end
                            )
                        } else null
                    } else null
                }
                cache[videoId] = segments
                segments
            } else {
                // 404 or other status code indicates no segments found
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("SponsorBlock", "Failed to fetch segments for $videoId: ${e.message}")
            emptyList()
        }
    }
}
