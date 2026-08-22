package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.LocalPlaylistDao
import com.devson.vedtube.core.database.model.LocalPlaylistEntity
import com.devson.vedtube.core.database.model.PlaylistVideoCrossRef
import com.devson.vedtube.domain.model.LocalPlaylist
import com.devson.vedtube.domain.model.LocalPlaylistDetail
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val localPlaylistDao: LocalPlaylistDao,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<LocalPlaylist>> {
        return localPlaylistDao.getAllPlaylistsWithVideos()
            .map { list ->
                list.map { withVideos ->
                    LocalPlaylist(
                        id = withVideos.playlist.playlistId,
                        name = withVideos.playlist.name,
                        createdAt = withVideos.playlist.createdAt,
                        videoCount = withVideos.videos.size,
                        thumbnailUrl = withVideos.videos.firstOrNull()?.thumbnailUrl
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getPlaylistDetail(playlistId: String): Flow<LocalPlaylistDetail?> {
        return localPlaylistDao.getPlaylistWithVideos(playlistId)
            .map { withVideos ->
                withVideos?.let { pwv ->
                    LocalPlaylistDetail(
                        id = pwv.playlist.playlistId,
                        name = pwv.playlist.name,
                        createdAt = pwv.playlist.createdAt,
                        videos = pwv.videos.map { cv ->
                            Video(
                                id = cv.videoId,
                                title = cv.title,
                                uploaderName = cv.channelName,
                                thumbnailUrl = cv.thumbnailUrl,
                                durationSeconds = cv.durationSeconds
                            )
                        }
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getPlaylistIdsContainingVideo(videoId: String): Flow<List<String>> {
        return localPlaylistDao.getPlaylistIdsContainingVideo(videoId)
            .flowOn(ioDispatcher)
    }

    override suspend fun createPlaylist(name: String): String {
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val id = UUID.randomUUID().toString()
        withContext(ioDispatcher) {
            localPlaylistDao.insertPlaylist(
                LocalPlaylistEntity(
                    playlistId = id,
                    name = trimmed,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    override suspend fun deletePlaylist(playlistId: String) {
        withContext(ioDispatcher) {
            localPlaylistDao.deletePlaylist(playlistId)
        }
    }

    override suspend fun addVideoToPlaylist(playlistId: String, video: Video) {
        withContext(ioDispatcher) {
            localPlaylistDao.addVideoToPlaylist(
                PlaylistVideoCrossRef(
                    playlistId = playlistId,
                    videoId = video.id,
                    title = video.title,
                    channelName = video.uploaderName,
                    thumbnailUrl = video.thumbnailUrl ?: "",
                    durationSeconds = video.durationSeconds,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        withContext(ioDispatcher) {
            localPlaylistDao.removeVideoFromPlaylist(playlistId, videoId)
        }
    }
}
