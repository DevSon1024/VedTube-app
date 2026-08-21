package com.devson.vedtube.feature.home

import com.devson.vedtube.domain.model.ThemeSettings

data class HomeUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDatabaseReady: Boolean = false,
    val isNetworkReady: Boolean = false,
    val isPlayerReady: Boolean = false,
    val isLoading: Boolean = false
)
