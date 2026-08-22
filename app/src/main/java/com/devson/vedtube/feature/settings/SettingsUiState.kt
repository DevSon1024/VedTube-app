package com.devson.vedtube.feature.settings

import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.BackupSummary
import com.devson.vedtube.domain.model.ThemeSettings

data class SettingsUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isSponsorBlockEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportErrorMessage: String? = null,
    val isImporting: Boolean = false,
    val importSummary: BackupSummary? = null,
    val importErrorMessage: String? = null,
    val isClearingData: Boolean = false,
    val clearSuccessMessage: String? = null
)
