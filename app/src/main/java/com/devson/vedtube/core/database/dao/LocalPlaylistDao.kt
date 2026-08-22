package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.devson.vedtube.core.database.model.LocalPlaylistEntity
import com.devson.vedtube.core.database.model.LocalPlaylistWithVideos
import com.devson.vedtube.core.database.model.PlaylistVideoCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity)

    @Query("DELETE FROM local_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM local_playlists WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllPlaylists(profileId: String = "profile_default"): Flow<List<LocalPlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM local_playlists WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllPlaylistsWithVideos(profileId: String = "profile_default"): Flow<List<LocalPlaylistWithVideos>>

    @Transaction
    @Query("SELECT * FROM local_playlists WHERE profileId = :profileId ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsWithVideosSync(profileId: String = "profile_default"): List<LocalPlaylistWithVideos>

    @Transaction
    @Query("SELECT * FROM local_playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsWithVideosAllProfilesSync(): List<LocalPlaylistWithVideos>

    @Transaction
    @Query("SELECT * FROM local_playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithVideos(playlistId: String): Flow<LocalPlaylistWithVideos?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideoToPlaylist(crossRef: PlaylistVideoCrossRef)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String)

    @Query("SELECT pv.playlistId FROM playlist_videos pv INNER JOIN local_playlists lp ON pv.playlistId = lp.playlistId WHERE pv.videoId = :videoId AND lp.profileId = :profileId")
    fun getPlaylistIdsContainingVideo(videoId: String, profileId: String = "profile_default"): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM playlist_videos WHERE playlistId = :playlistId")
    fun getVideoCount(playlistId: String): Flow<Int>

    @Query("DELETE FROM local_playlists WHERE profileId = :profileId")
    suspend fun clearPlaylistsForProfile(profileId: String = "profile_default")

    @Query("DELETE FROM local_playlists")
    suspend fun clearAllPlaylists()

    @Query("DELETE FROM playlist_videos")
    suspend fun clearAllPlaylistVideos()
}
