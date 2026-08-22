package com.devson.vedtube.domain.model

/**
 * Domain representation of a sponsored or non-music segment from SponsorBlock.
 */
data class SponsorSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}
