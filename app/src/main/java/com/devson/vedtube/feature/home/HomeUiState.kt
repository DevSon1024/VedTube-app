package com.devson.vedtube.feature.home

import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.Video

data class HomeUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchResults: List<Video> = emptyList(),
    val feedVideos: List<Video> = emptyList(),
    val isLoadingFeed: Boolean = false,
    val isSearching: Boolean = false,
    val error: AppError? = null,
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDatabaseReady: Boolean = true,
    val isNetworkReady: Boolean = true,
    val isPlayerReady: Boolean = true
) {
    val isLoading: Boolean
        get() = isLoadingFeed || isSearching

    val isSearchMode: Boolean
        get() = searchQuery.isNotBlank() || isSearchActive

    val displayVideos: List<Video>
        get() = if (isSearchMode) searchResults else feedVideos
}
