package com.devson.vedtube.feature.library

import com.devson.vedtube.domain.model.ChannelSubscription
import com.devson.vedtube.domain.model.WatchHistoryItem

data class LibraryUiState(
    val historyList: List<WatchHistoryItem> = emptyList(),
    val subscriptionsList: List<ChannelSubscription> = emptyList(),
    val isLoading: Boolean = false
)
