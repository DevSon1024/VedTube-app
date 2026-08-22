package com.devson.vedtube.data.provider.ryd

import retrofit2.http.GET
import retrofit2.http.Query

interface RydApiService {

    @GET("votes")
    suspend fun getVotes(
        @Query("videoId") videoId: String
    ): RydResponseDto
}
