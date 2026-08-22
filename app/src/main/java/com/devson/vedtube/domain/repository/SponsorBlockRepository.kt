package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.SponsorSegment

/**
 * Repository for retrieving SponsorBlock skip segments.
 */
interface SponsorBlockRepository {
    suspend fun getSegments(videoId: String): List<SponsorSegment>
}
