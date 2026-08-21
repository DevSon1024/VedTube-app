package com.devson.vedtube.feature.home

import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.domain.model.ThemeSettings

data class HomeUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDatabaseReady: Boolean = false,
    val isNetworkReady: Boolean = false,
    val isPlayerReady: Boolean = false,
    val rawIncomingUrl: String? = null,
    val parsedMediaUrl: ParsedMediaUrl? = null,
    val isLoading: Boolean = false
)
