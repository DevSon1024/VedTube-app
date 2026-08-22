package com.devson.vedtube.domain.provider

import com.devson.vedtube.domain.model.Channel
import com.devson.vedtube.domain.model.PagedResult
import com.devson.vedtube.domain.model.Playlist
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails

/**
 * High-level provider interface for media metadata extraction and discovery.
 */
interface MediaProvider {
    suspend fun search(query: String, pageToken: String? = null): Result<PagedResult<Video>>
    suspend fun getVideoDetails(videoId: String): Result<VideoDetails>
    suspend fun getChannelDetails(channelIdOrHandle: String): Result<Channel>
    suspend fun getPlaylistDetails(playlistId: String): Result<Playlist>
    suspend fun getComments(videoId: String, pageToken: String? = null): Result<PagedResult<com.devson.vedtube.domain.model.Comment>>
}
