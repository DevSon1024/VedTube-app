package com.devson.vedtube.data.provider.youtube.piped

import com.devson.vedtube.data.provider.youtube.piped.model.PipedStreamResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit interface for querying Piped instances as a resilient stream resolution fallback.
 */
interface PipedApiService {

    @GET
    suspend fun getStreamInfoFromUrl(
        @Url url: String
    ): Response<PipedStreamResponse>

    @GET("streams/{videoId}")
    suspend fun getStreamInfo(
        @Path("videoId") videoId: String
    ): Response<PipedStreamResponse>
}
