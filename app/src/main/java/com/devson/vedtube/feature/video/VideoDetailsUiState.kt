package com.devson.vedtube.feature.video

import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.DownloadItem
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
    val isLoadingDownloadStreams: Boolean = false
)
