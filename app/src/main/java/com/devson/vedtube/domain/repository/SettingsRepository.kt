package com.devson.vedtube.domain.repository

import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeSettings: Flow<ThemeSettings>
    val sponsorBlockEnabled: Flow<Boolean>
    val skipIntervalSeconds: Flow<Int>
    val distractionFreeMode: Flow<Boolean>
    val activeProfileId: Flow<String>
    val contentRegion: Flow<String>
    val appLanguage: Flow<String>

    suspend fun setThemeConfig(themeConfig: AppThemeConfig)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSponsorBlockEnabled(enabled: Boolean)
    suspend fun setSkipIntervalSeconds(seconds: Int)
    suspend fun setDistractionFreeMode(enabled: Boolean)
    suspend fun setActiveProfileId(profileId: String)
    suspend fun setContentRegion(region: String)
    suspend fun setAppLanguage(languageCode: String)
}
