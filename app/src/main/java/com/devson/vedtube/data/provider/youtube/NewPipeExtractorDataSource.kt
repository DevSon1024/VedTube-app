package com.devson.vedtube.data.provider.youtube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [YoutubeExtractorDataSource] backed by NewPipeExtractor.
 * Initialized with default localization and content country to prevent reload challenge blocks.
 */
@Singleton
class NewPipeExtractorDataSource @Inject constructor(
    downloader: NewPipeDownloader
) : YoutubeExtractorDataSource {

    init {
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
    }

    override suspend fun extractStreamInfo(videoId: String): StreamInfo {
        val url = if (videoId.startsWith("http://") || videoId.startsWith("https://")) {
            videoId
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }
        return StreamInfo.getInfo(ServiceList.YouTube, url)
    }

    override suspend fun extractSearchInfo(query: String, pageToken: String?): SearchInfo {
        val service = ServiceList.YouTube
        val searchQH = service.searchQHFactory.fromQuery(query)
        return SearchInfo.getInfo(service, searchQH)
    }

    override suspend fun extractTrending(region: String?): org.schabi.newpipe.extractor.kiosk.KioskInfo {
        val service = ServiceList.YouTube
        val kioskId = service.kioskList.defaultKioskId
        return org.schabi.newpipe.extractor.kiosk.KioskInfo.getInfo(service, kioskId)
    }

    override suspend fun extractChannelInfo(channelIdOrHandle: String): ChannelInfo {
        val url = when {
            channelIdOrHandle.startsWith("http://") || channelIdOrHandle.startsWith("https://") -> channelIdOrHandle
            channelIdOrHandle.startsWith("@") -> "https://www.youtube.com/$channelIdOrHandle"
            channelIdOrHandle.startsWith("UC") -> "https://www.youtube.com/channel/$channelIdOrHandle"
            else -> "https://www.youtube.com/@$channelIdOrHandle"
        }
        return ChannelInfo.getInfo(ServiceList.YouTube, url)
    }

    override suspend fun extractPlaylistInfo(playlistId: String): PlaylistInfo {
        val url = if (playlistId.startsWith("http://") || playlistId.startsWith("https://")) {
            playlistId
        } else {
            "https://www.youtube.com/playlist?list=$playlistId"
        }
        return PlaylistInfo.getInfo(ServiceList.YouTube, url)
    }

    override suspend fun extractComments(videoId: String, pageToken: String?): org.schabi.newpipe.extractor.comments.CommentsInfo {
        val url = if (videoId.startsWith("http://") || videoId.startsWith("https://")) {
            videoId
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }
        return org.schabi.newpipe.extractor.comments.CommentsInfo.getInfo(ServiceList.YouTube, url)
    }
}
