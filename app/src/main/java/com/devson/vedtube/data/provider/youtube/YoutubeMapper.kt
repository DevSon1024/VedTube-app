package com.devson.vedtube.data.provider.youtube

import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.data.provider.youtube.url.YoutubeUrlParser
import com.devson.vedtube.domain.model.Channel
import com.devson.vedtube.domain.model.PagedResult
import com.devson.vedtube.domain.model.Playlist
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Maps NewPipeExtractor objects to domain models with safe null and edge-case handling.
 */
object YoutubeMapper {

    fun mapStreamInfoToVideoDetails(info: StreamInfo, fallbackId: String? = null): VideoDetails {
        val videoId = extractVideoId(info.url, fallbackId ?: info.id)
        val bestThumbnail = info.thumbnails.orEmpty().maxByOrNull { it.width }?.url
            ?: info.thumbnails.orEmpty().firstOrNull()?.url

        val uploaderAvatar = info.uploaderAvatars.orEmpty().maxByOrNull { it.width }?.url
            ?: info.uploaderAvatars.orEmpty().firstOrNull()?.url

        val related = info.relatedItems.orEmpty().mapNotNull { item ->
            if (item is StreamInfoItem) {
                mapStreamInfoItemToVideo(item)
            } else {
                null
            }
        }

        val isLiveStream = info.streamType == StreamType.LIVE_STREAM ||
                info.streamType == StreamType.AUDIO_LIVE_STREAM

        return VideoDetails(
            id = videoId,
            title = info.name.orEmpty(),
            description = info.description?.content ?: "",
            uploaderName = info.uploaderName.orEmpty(),
            uploaderId = extractChannelId(info.uploaderUrl),
            uploaderAvatarUrl = uploaderAvatar,
            thumbnailUrl = bestThumbnail,
            durationSeconds = if (info.duration > 0) info.duration else 0L,
            viewCount = if (info.viewCount >= 0) info.viewCount else 0L,
            likeCount = if (info.likeCount >= 0) info.likeCount else null,
            uploadDate = info.textualUploadDate ?: info.uploadDate?.offsetDateTime()?.toString(),
            isLive = isLiveStream,
            subscriberCount = info.uploaderSubscriberCount.takeIf { it >= 0 },
            relatedVideos = related
        )
    }

    fun mapStreamInfoItemToVideo(item: StreamInfoItem): Video {
        val videoId = extractVideoId(item.url, null)
        val bestThumbnail = item.thumbnails.orEmpty().maxByOrNull { it.width }?.url
            ?: item.thumbnails.orEmpty().firstOrNull()?.url

        val uploaderAvatar = item.uploaderAvatars.orEmpty().maxByOrNull { it.width }?.url
            ?: item.uploaderAvatars.orEmpty().firstOrNull()?.url

        return Video(
            id = videoId,
            title = item.name.orEmpty(),
            uploaderName = item.uploaderName.orEmpty(),
            uploaderId = extractChannelId(item.uploaderUrl),
            uploaderAvatarUrl = uploaderAvatar,
            thumbnailUrl = bestThumbnail,
            durationSeconds = if (item.duration > 0) item.duration else 0L,
            viewCount = if (item.viewCount >= 0) item.viewCount else 0L,
            uploadDate = item.textualUploadDate ?: item.uploadDate?.offsetDateTime()?.toString()
        )
    }

    fun mapSearchInfoToPagedResult(searchInfo: SearchInfo): PagedResult<Video> {
        val videos = searchInfo.relatedItems.orEmpty().mapNotNull { item ->
            if (item is StreamInfoItem) {
                mapStreamInfoItemToVideo(item)
            } else {
                null
            }
        }
        val nextToken = searchInfo.nextPage?.url ?: searchInfo.nextPage?.id
        return PagedResult(
            items = videos,
            nextPageToken = nextToken
        )
    }

    fun mapKioskInfoToPagedResult(kioskInfo: org.schabi.newpipe.extractor.kiosk.KioskInfo): PagedResult<Video> {
        val videos = kioskInfo.relatedItems.orEmpty().mapNotNull { item ->
            if (item is StreamInfoItem) {
                mapStreamInfoItemToVideo(item)
            } else {
                null
            }
        }
        val nextToken = if (kioskInfo.hasNextPage()) kioskInfo.nextPage?.url ?: kioskInfo.nextPage?.id else null
        return PagedResult(
            items = videos,
            nextPageToken = nextToken
        )
    }

    fun mapChannelInfoToChannel(channelInfo: ChannelInfo): Channel {
        val avatar = channelInfo.avatars.orEmpty().maxByOrNull { it.width }?.url
            ?: channelInfo.avatars.orEmpty().firstOrNull()?.url
        val banner = channelInfo.banners.orEmpty().maxByOrNull { it.width }?.url
            ?: channelInfo.banners.orEmpty().firstOrNull()?.url

        return Channel(
            id = channelInfo.id.orEmpty().ifBlank { extractChannelId(channelInfo.url).orEmpty() },
            name = channelInfo.name.orEmpty(),
            handle = extractHandle(channelInfo.url),
            avatarUrl = avatar,
            bannerUrl = banner,
            subscriberCount = channelInfo.subscriberCount.takeIf { it >= 0 },
            description = channelInfo.description
        )
    }

    fun mapPlaylistInfoToPlaylist(playlistInfo: PlaylistInfo): Playlist {
        val thumbnail = playlistInfo.thumbnails.orEmpty().maxByOrNull { it.width }?.url
            ?: playlistInfo.thumbnails.orEmpty().firstOrNull()?.url
            ?: playlistInfo.banners.orEmpty().firstOrNull()?.url

        val videos = playlistInfo.relatedItems.orEmpty().mapNotNull { item ->
            if (item is StreamInfoItem) {
                mapStreamInfoItemToVideo(item)
            } else {
                null
            }
        }

        val resolvedId = playlistInfo.id.orEmpty().ifBlank {
            if (playlistInfo.url.contains("list=")) {
                playlistInfo.url.substringAfter("list=").substringBefore("&")
            } else {
                ""
            }
        }

        return Playlist(
            id = resolvedId,
            title = playlistInfo.name.orEmpty(),
            uploaderName = playlistInfo.uploaderName,
            thumbnailUrl = thumbnail,
            videoCount = if (playlistInfo.streamCount >= 0) playlistInfo.streamCount else videos.size.toLong(),
            videos = videos
        )
    }

    fun mapCommentsInfoToPagedResult(
        commentsInfo: org.schabi.newpipe.extractor.comments.CommentsInfo
    ): PagedResult<com.devson.vedtube.domain.model.Comment> {
        val comments = commentsInfo.relatedItems.orEmpty().mapNotNull { item ->
            if (item is org.schabi.newpipe.extractor.comments.CommentsInfoItem) {
                val avatar = item.uploaderAvatars.orEmpty().maxByOrNull { it.width }?.url
                    ?: item.uploaderAvatars.orEmpty().firstOrNull()?.url
                    ?: item.thumbnails.orEmpty().firstOrNull()?.url
                com.devson.vedtube.domain.model.Comment(
                    id = item.commentId ?: item.url ?: java.util.UUID.randomUUID().toString(),
                    authorName = item.uploaderName.orEmpty().ifBlank { "Anonymous" },
                    authorAvatarUrl = avatar,
                    commentText = item.commentText?.content ?: item.name ?: "",
                    likeCount = if (item.likeCount >= 0) item.likeCount.toLong() else 0L,
                    publishDate = item.textualUploadDate,
                    replyCount = if (item.replyCount >= 0) item.replyCount else 0
                )
            } else {
                null
            }
        }
        val nextPageToken = if (commentsInfo.hasNextPage()) commentsInfo.nextPage?.url ?: commentsInfo.nextPage?.id else null
        return PagedResult(
            items = comments,
            nextPageToken = nextPageToken,
            totalResults = commentsInfo.commentsCount.toLong().takeIf { it >= 0 }
        )
    }

    private fun extractVideoId(url: String?, fallbackId: String?): String {
        if (!fallbackId.isNullOrBlank()) {
            return fallbackId
        }
        if (!url.isNullOrBlank()) {
            val parsed = YoutubeUrlParser.parse(url)
            if (parsed is ParsedMediaUrl.Video) {
                return parsed.videoId
            }
            if (url.contains("v=")) {
                return url.substringAfter("v=").substringBefore("&")
            }
            if (url.contains("/shorts/")) {
                return url.substringAfter("/shorts/").substringBefore("/").substringBefore("?")
            }
            if (url.contains("youtu.be/")) {
                return url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            }
        }
        return fallbackId.orEmpty()
    }

    private fun extractChannelId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val parsed = YoutubeUrlParser.parse(url)
        return when (parsed) {
            is ParsedMediaUrl.Channel.Id -> parsed.channelId
            is ParsedMediaUrl.Channel.Handle -> parsed.handle
            is ParsedMediaUrl.Channel.User -> parsed.username
            is ParsedMediaUrl.Channel.CustomUrl -> parsed.customUrl
            else -> {
                if (url.contains("/channel/")) {
                    url.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                } else if (url.contains("/@")) {
                    "@" + url.substringAfter("/@").substringBefore("/").substringBefore("?")
                } else {
                    null
                }
            }
        }
    }

    private fun extractHandle(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val parsed = YoutubeUrlParser.parse(url)
        return if (parsed is ParsedMediaUrl.Channel.Handle) {
            parsed.handle
        } else if (url.contains("/@")) {
            "@" + url.substringAfter("/@").substringBefore("/").substringBefore("?")
        } else {
            null
        }
    }
}
