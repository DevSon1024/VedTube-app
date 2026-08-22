package com.devson.vedtube.data.provider.sponsorblock

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for querying SponsorBlock API (https://sponsor.ajay.app/).
 */
interface SponsorBlockApiService {

    @GET("api/skipSegments")
    suspend fun getSkipSegments(
        @Query("videoID") videoId: String,
        @Query("categories") categories: String? = "[\"sponsor\",\"intro\",\"outro\",\"interaction\",\"selfpromo\",\"preview\"]"
    ): Response<List<SponsorBlockSegmentResponse>>
}
