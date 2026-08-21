package com.devson.vedtube.domain.repository

import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeSettings: Flow<ThemeSettings>
    suspend fun setThemeConfig(themeConfig: AppThemeConfig)
    suspend fun setDynamicColor(enabled: Boolean)
}
