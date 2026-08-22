package com.devson.vedtube.domain.repository

interface RydRepository {
    /**
     * Asynchronously fetches the Return YouTube Dislike (RYD) dislike count for a video.
     * Safely returns a failure result or null if the video has no dislikes / API fails / rate limits.
     */
    suspend fun getDislikes(videoId: String): Result<Long?>
}
