package com.devson.vedtube.data.provider.youtube.piped

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit interface for querying Piped instances returning raw [ResponseBody]
 * to enable robust manual JSON deserialization and plain-text/HTML guard checking.
 */
interface PipedApiService {

    @GET
    suspend fun getStreamInfoFromUrl(
        @Url url: String
    ): Response<ResponseBody>

    @GET("streams/{videoId}")
    suspend fun getStreamInfo(
        @Path("videoId") videoId: String
    ): Response<ResponseBody>
}
