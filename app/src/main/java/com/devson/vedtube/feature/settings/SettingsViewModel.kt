package com.devson.vedtube.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.BackupSummary
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.UserProfile
import com.devson.vedtube.domain.repository.DataManagementRepository
import com.devson.vedtube.domain.repository.SettingsRepository
import com.devson.vedtube.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataManagementRepository: DataManagementRepository,
    private val userProfileRepository: UserProfileRepository,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _exportImportState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.themeSettings,
            settingsRepository.sponsorBlockEnabled,
            settingsRepository.skipIntervalSeconds,
            settingsRepository.distractionFreeMode,
            settingsRepository.contentRegion
        ) { theme, sponsorBlock, skipInterval, distractionFree, region ->
            ThemeAndPlaybackConfig(theme, sponsorBlock, skipInterval, distractionFree, region)
        },
        combine(
            settingsRepository.appLanguage,
            userProfileRepository.allProfiles,
            userProfileRepository.activeProfileId,
            userProfileRepository.activeProfile
        ) { language, profiles, activeId, activeProf ->
            ProfileAndLanguageConfig(language, profiles, activeId, activeProf)
        },
        _exportImportState
    ) { playbackConfig, profileAndLangConfig, customState ->
        customState.copy(
            themeSettings = playbackConfig.theme,
            isSponsorBlockEnabled = playbackConfig.sponsorBlock,
            skipIntervalSeconds = playbackConfig.skipInterval,
            isDistractionFreeMode = playbackConfig.distractionFree,
            contentRegion = playbackConfig.region,
            appLanguage = profileAndLangConfig.language,
            profiles = profileAndLangConfig.profiles,
            activeProfileId = profileAndLangConfig.activeId,
            activeProfile = profileAndLangConfig.activeProfile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeConfig(config: AppThemeConfig) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setThemeConfig(config)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSponsorBlockEnabled(enabled)
        }
    }

    fun setSkipIntervalSeconds(seconds: Int) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSkipIntervalSeconds(seconds)
        }
    }

    fun setDistractionFreeMode(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDistractionFreeMode(enabled)
        }
    }

    fun setContentRegion(region: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setContentRegion(region)
        }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAppLanguage(languageCode)
        }
    }

    fun setActiveProfile(profileId: String) {
        viewModelScope.launch(ioDispatcher) {
            userProfileRepository.setActiveProfile(profileId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch(ioDispatcher) {
            val newId = userProfileRepository.createProfile(name)
            userProfileRepository.setActiveProfile(newId)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch(ioDispatcher) {
            userProfileRepository.deleteProfile(profileId)
        }
    }

    fun exportData(outputStream: OutputStream) {
        _exportImportState.update {
            it.copy(
                isExporting = true,
                exportSuccessMessage = null,
                exportErrorMessage = null
            )
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                dataManagementRepository.exportData(outputStream)
            }
            result.onSuccess { summary ->
                _exportImportState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccessMessage = "Data successfully exported! (${summary.subscriptionsCount} subs, ${summary.watchHistoryCount} history, ${summary.playlistsCount} playlists)"
                    )
                }
            }.onFailure { error ->
                _exportImportState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = error.message ?: "Failed to export data"
                    )
                }
            }
        }
    }

    fun importData(inputStream: InputStream) {
        _exportImportState.update {
            it.copy(
                isImporting = true,
                importSummary = null,
                importErrorMessage = null
            )
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                dataManagementRepository.importData(inputStream)
            }
            result.onSuccess { summary ->
                _exportImportState.update {
                    it.copy(
                        isImporting = false,
                        importSummary = summary
                    )
                }
            }.onFailure { error ->
                _exportImportState.update {
                    it.copy(
                        isImporting = false,
                        importErrorMessage = error.message ?: "Failed to import data"
                    )
                }
            }
        }
    }

    fun clearAllData() {
        _exportImportState.update {
            it.copy(
                isClearingData = true,
                clearSuccessMessage = null
            )
        }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                dataManagementRepository.clearAllData()
            }
            _exportImportState.update {
                it.copy(
                    isClearingData = false,
                    clearSuccessMessage = "All local data has been successfully cleared."
                )
            }
        }
    }

    fun dismissMessages() {
        _exportImportState.update {
            it.copy(
                exportSuccessMessage = null,
                exportErrorMessage = null,
                importSummary = null,
                importErrorMessage = null,
                clearSuccessMessage = null
            )
        }
    }

    private data class ThemeAndPlaybackConfig(
        val theme: ThemeSettings,
        val sponsorBlock: Boolean,
        val skipInterval: Int,
        val distractionFree: Boolean,
        val region: String
    )

    private data class ProfileAndLanguageConfig(
        val language: String,
        val profiles: List<UserProfile>,
        val activeId: String,
        val activeProfile: UserProfile?
    )
}
