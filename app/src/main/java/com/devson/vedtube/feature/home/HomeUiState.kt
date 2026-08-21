package com.devson.vedtube.feature.home

import com.devson.vedtube.core.player.PlayerState
import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.domain.model.PlaybackSource
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.VideoStream

data class HomeUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDatabaseReady: Boolean = false,
    val isNetworkReady: Boolean = false,
    val isPlayerReady: Boolean = false,
    val rawIncomingUrl: String? = null,
    val parsedMediaUrl: ParsedMediaUrl? = null,
    val isLoading: Boolean = false,
    val playerState: PlayerState = PlayerState()
)
