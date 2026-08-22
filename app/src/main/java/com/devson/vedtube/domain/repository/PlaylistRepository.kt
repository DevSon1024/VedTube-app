package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.LocalPlaylist
import com.devson.vedtube.domain.model.LocalPlaylistDetail
import com.devson.vedtube.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<LocalPlaylist>>
    fun getPlaylistDetail(playlistId: String): Flow<LocalPlaylistDetail?>
    fun getPlaylistIdsContainingVideo(videoId: String): Flow<List<String>>
    suspend fun createPlaylist(name: String): String
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addVideoToPlaylist(playlistId: String, video: Video)
    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String)
}
