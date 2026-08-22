package com.devson.vedtube.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation combining a playlist with all its associated videos.
 */
data class LocalPlaylistWithVideos(
    @Embedded
    val playlist: LocalPlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "playlistId"
    )
    val videos: List<PlaylistVideoCrossRef>
)
