package com.devson.vedtube.data.provider.youtube.cobalt

import com.devson.vedtube.data.provider.youtube.cobalt.model.CobaltRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit interface for querying Cobalt API instances.
 */
interface CobaltApiService {

    @POST
    suspend fun resolveStream(
        @Url url: String,
        @Body request: CobaltRequest,
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/json"
    ): Response<ResponseBody>
}
