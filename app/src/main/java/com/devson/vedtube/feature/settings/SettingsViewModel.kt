package com.devson.vedtube.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.repository.DataManagementRepository
import com.devson.vedtube.domain.repository.SettingsRepository
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
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _exportImportState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeSettings,
        settingsRepository.sponsorBlockEnabled,
        _exportImportState
    ) { theme, sponsorBlock, customState ->
        customState.copy(
            themeSettings = theme,
            isSponsorBlockEnabled = sponsorBlock
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
}
