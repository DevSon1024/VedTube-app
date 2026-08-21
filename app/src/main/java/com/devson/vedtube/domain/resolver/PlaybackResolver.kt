package com.devson.vedtube.domain.resolver

import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.PlaybackSource

/**
 * Domain interface for resolving playable stream assets for a video.
 */
interface PlaybackResolver {
    suspend fun resolve(
        videoId: String,
        preferences: PlaybackPreferences = PlaybackPreferences()
    ): Result<PlaybackSource>
}
