package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.VedTubeDatabase
import com.devson.vedtube.core.database.model.LocalPlaylistEntity
import com.devson.vedtube.core.database.model.PlaylistVideoCrossRef
import com.devson.vedtube.core.database.model.SearchHistoryEntity
import com.devson.vedtube.core.database.model.SubscriptionEntity
import com.devson.vedtube.core.database.model.WatchHistoryEntity
import com.devson.vedtube.data.model.backup.PlaylistBackupDto
import com.devson.vedtube.data.model.backup.PlaylistVideoBackupDto
import com.devson.vedtube.data.model.backup.SearchHistoryBackupDto
import com.devson.vedtube.data.model.backup.SubscriptionBackupDto
import com.devson.vedtube.data.model.backup.VedTubeBackupDto
import com.devson.vedtube.data.model.backup.WatchHistoryBackupDto
import com.devson.vedtube.domain.model.BackupSummary
import com.devson.vedtube.domain.repository.DataManagementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagementRepositoryImpl @Inject constructor(
    private val database: VedTubeDatabase,
    private val json: Json,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DataManagementRepository {

    override suspend fun exportData(outputStream: OutputStream): Result<BackupSummary> = withContext(ioDispatcher) {
        runCatching {
            val subscriptions = database.subscriptionDao().getAllSubscriptionsSync()
            val watchHistory = database.watchHistoryDao().getAllHistorySync()
            val playlistsWithVideos = database.localPlaylistDao().getAllPlaylistsWithVideosSync()
            val searchHistory = database.searchHistoryDao().getAllQueriesSync()

            val backupDto = VedTubeBackupDto(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                subscriptions = subscriptions.map {
                    SubscriptionBackupDto(
                        channelId = it.channelId,
                        channelName = it.channelName,
                        avatarUrl = it.avatarUrl,
                        subscribedAt = it.subscribedAt
                    )
                },
                watchHistory = watchHistory.map {
                    WatchHistoryBackupDto(
                        videoId = it.videoId,
                        title = it.title,
                        channelName = it.channelName,
                        thumbnailUrl = it.thumbnailUrl,
                        durationMs = it.durationMs,
                        progressMs = it.progressMs,
                        lastWatchedAt = it.lastWatchedAt
                    )
                },
                playlists = playlistsWithVideos.map { pwv ->
                    PlaylistBackupDto(
                        playlistId = pwv.playlist.playlistId,
                        name = pwv.playlist.name,
                        createdAt = pwv.playlist.createdAt,
                        videos = pwv.videos.map { v ->
                            PlaylistVideoBackupDto(
                                videoId = v.videoId,
                                title = v.title,
                                channelName = v.channelName,
                                thumbnailUrl = v.thumbnailUrl,
                                durationSeconds = v.durationSeconds,
                                addedAt = v.addedAt
                            )
                        }
                    )
                },
                searchHistory = searchHistory.map {
                    SearchHistoryBackupDto(
                        query = it.query,
                        timestamp = it.timestamp
                    )
                }
            )

            val jsonString = json.encodeToString(backupDto)
            outputStream.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            var totalVideosInPlaylists = 0
            backupDto.playlists.forEach { totalVideosInPlaylists += it.videos.size }

            BackupSummary(
                subscriptionsCount = backupDto.subscriptions.size,
                watchHistoryCount = backupDto.watchHistory.size,
                playlistsCount = backupDto.playlists.size,
                playlistVideosCount = totalVideosInPlaylists,
                searchHistoryCount = backupDto.searchHistory.size
            )
        }
    }

    override suspend fun importData(inputStream: InputStream): Result<BackupSummary> = withContext(ioDispatcher) {
        runCatching {
            val jsonString = inputStream.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
            val backupDto = json.decodeFromString<VedTubeBackupDto>(jsonString)

            var totalVideosInPlaylists = 0

            // Execute in transaction for atomicity and data safety
            database.runInTransaction {
                // Subscriptions
                backupDto.subscriptions.forEach { sub ->
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO subscriptions (channelId, channelName, avatarUrl, subscribedAt) VALUES (?, ?, ?, ?)",
                        arrayOf(sub.channelId, sub.channelName, sub.avatarUrl, sub.subscribedAt)
                    )
                }

                // Watch History
                backupDto.watchHistory.forEach { hist ->
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO watch_history (videoId, title, channelName, thumbnailUrl, durationMs, progressMs, lastWatchedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(hist.videoId, hist.title, hist.channelName, hist.thumbnailUrl, hist.durationMs, hist.progressMs, hist.lastWatchedAt)
                    )
                }

                // Playlists & Playlist Videos
                backupDto.playlists.forEach { pl ->
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO local_playlists (playlistId, name, createdAt) VALUES (?, ?, ?)",
                        arrayOf(pl.playlistId, pl.name, pl.createdAt)
                    )

                    pl.videos.forEach { v ->
                        totalVideosInPlaylists++
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT OR REPLACE INTO playlist_videos (playlistId, videoId, title, channelName, thumbnailUrl, durationSeconds, addedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(pl.playlistId, v.videoId, v.title, v.channelName, v.thumbnailUrl, v.durationSeconds, v.addedAt)
                        )
                    }
                }

                // Search History
                backupDto.searchHistory.forEach { sh ->
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO search_history (`query`, `timestamp`) VALUES (?, ?)",
                        arrayOf(sh.query, sh.timestamp)
                    )
                }
            }

            BackupSummary(
                subscriptionsCount = backupDto.subscriptions.size,
                watchHistoryCount = backupDto.watchHistory.size,
                playlistsCount = backupDto.playlists.size,
                playlistVideosCount = totalVideosInPlaylists,
                searchHistoryCount = backupDto.searchHistory.size
            )
        }
    }

    override suspend fun clearAllData(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            database.subscriptionDao().clearAll()
            database.watchHistoryDao().clearHistory()
            database.localPlaylistDao().clearAllPlaylists()
            database.localPlaylistDao().clearAllPlaylistVideos()
            database.searchHistoryDao().clearAll()
        }
    }
}
