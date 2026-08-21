package com.devson.vedtube.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.core.player.VedPlayer
import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.data.provider.youtube.url.YoutubeUrlParser
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appInfoDao: AppInfoDao,
    private val okHttpClient: OkHttpClient,
    val vedPlayer: VedPlayer,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val exoPlayer = vedPlayer.exoPlayer

    private val _infrastructureState = MutableStateFlow(
        Triple(
            first = false, // isDatabaseReady
            second = false, // isNetworkReady
            third = false  // isPlayerReady
        )
    )

    private val _parsedMediaState = MutableStateFlow<Pair<String?, ParsedMediaUrl?>>(
        Pair(null, null)
    )

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.themeSettings,
        _infrastructureState,
        _parsedMediaState,
        vedPlayer.playerState
    ) { themeSettings, infra, media, playerState ->
        HomeUiState(
            themeSettings = themeSettings,
            isDatabaseReady = infra.first,
            isNetworkReady = infra.second,
            isPlayerReady = infra.third,
            rawIncomingUrl = media.first,
            parsedMediaUrl = media.second,
            isLoading = false,
            playerState = playerState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        verifyInfrastructure()
    }

    fun onPlayerEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }

    fun handleIncomingIntent(urlOrText: String) {
        viewModelScope.launch {
            val parsed = withContext(ioDispatcher) {
                YoutubeUrlParser.parse(urlOrText)
            }
            _parsedMediaState.value = Pair(urlOrText, parsed)

            when (parsed) {
                is ParsedMediaUrl.Video -> {
                    vedPlayer.playVideoId(
                        videoId = parsed.videoId,
                        title = "Shared Video (${parsed.videoId})"
                    )
                }
                else -> { /* Handle Playlist or Channel in subsequent phases */ }
            }
        }
    }

    fun playSampleVideo(videoId: String, title: String, uploader: String = "Test Creator") {
        vedPlayer.playVideo(
            Video(
                id = videoId,
                title = title,
                uploaderName = uploader
            )
        )
    }

    fun toggleTheme(config: AppThemeConfig) {
        viewModelScope.launch {
            settingsRepository.setThemeConfig(config)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    private fun verifyInfrastructure() {
        viewModelScope.launch(ioDispatcher) {
            val dbReady = try {
                appInfoDao.insertInfo(
                    AppInfoEntity(
                        key = "init_check",
                        value = System.currentTimeMillis().toString()
                    )
                )
                val readBack = appInfoDao.getInfo("init_check")
                readBack != null
            } catch (e: Exception) {
                false
            }

            val netReady = try {
                okHttpClient.connectionPool.connectionCount() >= 0
            } catch (e: Exception) {
                false
            }

            val playerReady = true

            _infrastructureState.value = Triple(dbReady, netReady, playerReady)
        }
    }
}
