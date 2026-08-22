package com.devson.vedtube.feature.video

import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.Comment
import com.devson.vedtube.domain.model.DownloadItem
import com.devson.vedtube.domain.model.LocalPlaylist
import com.devson.vedtube.domain.model.VideoDetails
import com.devson.vedtube.domain.model.VideoStream

data class VideoDetailsUiState(
    val videoId: String = "",
    val details: VideoDetails? = null,
    val isLoadingDetails: Boolean = false,
    val error: AppError? = null,
    val isDescriptionExpanded: Boolean = false,
    val isSubscribed: Boolean = false,
    val watchProgressMap: Map<String, Float> = emptyMap(),
    val downloadItem: DownloadItem? = null,
    val isDownloadQualitySheetVisible: Boolean = false,
    val availableDownloadStreams: List<VideoStream> = emptyList(),
    val isLoadingDownloadStreams: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentsNextPageToken: String? = null,
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentsError: String? = null,
    val totalCommentsCount: Long? = null,
    val isCommentsSheetVisible: Boolean = false,
    val isSaveToPlaylistSheetVisible: Boolean = false,
    val playlists: List<LocalPlaylist> = emptyList(),
    val containingPlaylistIds: List<String> = emptyList(),
    val dislikesCount: Long? = null,
    val isDistractionFreeMode: Boolean = false
)
