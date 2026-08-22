package com.devson.vedtube.data.provider.youtube

import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Data source abstraction for YouTube metadata extraction.
 */
interface YoutubeExtractorDataSource {
    suspend fun extractStreamInfo(videoId: String): StreamInfo
    suspend fun extractSearchInfo(query: String, pageToken: String? = null): SearchInfo
    suspend fun extractChannelInfo(channelIdOrHandle: String): ChannelInfo
    suspend fun extractPlaylistInfo(playlistId: String): PlaylistInfo
    suspend fun extractComments(videoId: String, pageToken: String? = null): org.schabi.newpipe.extractor.comments.CommentsInfo
}
