package com.devson.vedtube.feature.settings

import com.devson.vedtube.domain.model.BackupSummary
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.UserProfile

data class SettingsUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isSponsorBlockEnabled: Boolean = true,
    val skipIntervalSeconds: Int = 10,
    val isDistractionFreeMode: Boolean = false,
    val profiles: List<UserProfile> = emptyList(),
    val activeProfileId: String = UserProfile.DEFAULT_PROFILE_ID,
    val activeProfile: UserProfile? = null,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportErrorMessage: String? = null,
    val isImporting: Boolean = false,
    val importSummary: BackupSummary? = null,
    val importErrorMessage: String? = null,
    val isClearingData: Boolean = false,
    val clearSuccessMessage: String? = null
)
