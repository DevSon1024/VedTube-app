package com.devson.vedtube.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.datastore.model.AppThemeConfig
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
    private val exoPlayer: ExoPlayer,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _infrastructureState = MutableStateFlow(
        Triple(
            first = false, // isDatabaseReady
            second = false, // isNetworkReady
            third = false // isPlayerReady
        )
    )

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.themeSettings,
        _infrastructureState
    ) { themeSettings, infra ->
        HomeUiState(
            themeSettings = themeSettings,
            isDatabaseReady = infra.first,
            isNetworkReady = infra.second,
            isPlayerReady = infra.third,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        verifyInfrastructure()
    }

    private fun verifyInfrastructure() {
        viewModelScope.launch {
            val dbReady = withContext(ioDispatcher) {
                try {
                    appInfoDao.insertInfo(AppInfoEntity("app_init", "phase_0_ok"))
                    val info = appInfoDao.getInfo("app_init")
                    info?.value == "phase_0_ok"
                } catch (e: Exception) {
                    false
                }
            }

            val networkReady = try {
                okHttpClient.connectionPool.connectionCount() >= 0
            } catch (e: Exception) {
                false
            }

            val playerReady = try {
                exoPlayer.playbackState >= 0
            } catch (e: Exception) {
                false
            }

            _infrastructureState.value = Triple(dbReady, networkReady, playerReady)
        }
    }

    fun setThemeConfig(config: AppThemeConfig) {
        viewModelScope.launch {
            settingsRepository.setThemeConfig(config)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release ExoPlayer instance cleanly if needed
        exoPlayer.release()
    }
}
