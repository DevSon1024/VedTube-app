package com.devson.vedtube.data.provider.youtube

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.domain.model.Channel
import com.devson.vedtube.domain.model.PagedResult
import com.devson.vedtube.domain.model.Playlist
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails
import com.devson.vedtube.domain.provider.MediaProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [MediaProvider] backed by the YouTube extraction engine.
 * Dispatches all network parsing and extraction operations onto [Dispatchers.IO].
 */
@Singleton
class YoutubeProviderImpl @Inject constructor(
    private val extractorDataSource: YoutubeExtractorDataSource,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MediaProvider {

    override suspend fun search(query: String, pageToken: String?): Result<PagedResult<Video>> =
        withContext(ioDispatcher) {
            try {
                val searchInfo = extractorDataSource.extractSearchInfo(query, pageToken)
                val pagedResult = YoutubeMapper.mapSearchInfoToPagedResult(searchInfo)
                Result.success(pagedResult)
            } catch (e: Throwable) {
                Result.failure(YoutubeErrorMapper.map(e))
            }
        }

    override suspend fun getVideoDetails(videoId: String): Result<VideoDetails> =
        withContext(ioDispatcher) {
            try {
                val streamInfo = extractorDataSource.extractStreamInfo(videoId)
                val details = YoutubeMapper.mapStreamInfoToVideoDetails(streamInfo, videoId)
                Result.success(details)
            } catch (e: Throwable) {
                Result.failure(YoutubeErrorMapper.map(e, videoId))
            }
        }

    override suspend fun getChannelDetails(channelIdOrHandle: String): Result<Channel> =
        withContext(ioDispatcher) {
            try {
                val channelInfo = extractorDataSource.extractChannelInfo(channelIdOrHandle)
                val channel = YoutubeMapper.mapChannelInfoToChannel(channelInfo)
                Result.success(channel)
            } catch (e: Throwable) {
                Result.failure(YoutubeErrorMapper.map(e, channelIdOrHandle))
            }
        }

    override suspend fun getPlaylistDetails(playlistId: String): Result<Playlist> =
        withContext(ioDispatcher) {
            try {
                val playlistInfo = extractorDataSource.extractPlaylistInfo(playlistId)
                val playlist = YoutubeMapper.mapPlaylistInfoToPlaylist(playlistInfo)
                Result.success(playlist)
            } catch (e: Throwable) {
                Result.failure(YoutubeErrorMapper.map(e, playlistId))
            }
        }
}
