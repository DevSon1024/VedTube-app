package com.devson.vedtube.feature.library

import com.devson.vedtube.domain.model.ChannelSubscription
import com.devson.vedtube.domain.model.DownloadItem
import com.devson.vedtube.domain.model.WatchHistoryItem

data class LibraryUiState(
    val historyList: List<WatchHistoryItem> = emptyList(),
    val subscriptionsList: List<ChannelSubscription> = emptyList(),
    val downloadsList: List<DownloadItem> = emptyList(),
    val playlists: List<com.devson.vedtube.domain.model.LocalPlaylist> = emptyList(),
    val isSponsorBlockEnabled: Boolean = true,
    val isLoading: Boolean = false
)
