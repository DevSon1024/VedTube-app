package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.data.provider.ryd.RydApiService
import com.devson.vedtube.domain.repository.RydRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RydRepositoryImpl @Inject constructor(
    private val rydApiService: RydApiService,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : RydRepository {

    private val cache = ConcurrentHashMap<String, Long>()

    override suspend fun getDislikes(videoId: String): Result<Long?> = withContext(ioDispatcher) {
        if (cache.containsKey(videoId)) {
            return@withContext Result.success(cache[videoId])
        }

        runCatching {
            val response = rydApiService.getVotes(videoId)
            val dislikes = response.dislikes
            if (dislikes != null) {
                cache[videoId] = dislikes
            }
            dislikes
        }
    }
}
