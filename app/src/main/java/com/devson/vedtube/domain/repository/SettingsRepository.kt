package com.devson.vedtube.domain.repository

import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeSettings: Flow<ThemeSettings>
    val sponsorBlockEnabled: Flow<Boolean>
    suspend fun setThemeConfig(themeConfig: AppThemeConfig)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSponsorBlockEnabled(enabled: Boolean)
}
