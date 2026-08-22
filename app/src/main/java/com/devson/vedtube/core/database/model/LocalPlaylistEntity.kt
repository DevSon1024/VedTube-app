package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a custom local user playlist.
 */
@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey
    val playlistId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
