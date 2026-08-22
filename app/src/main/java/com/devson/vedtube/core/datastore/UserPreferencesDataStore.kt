package com.devson.vedtube.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object PreferencesKeys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SPONSORBLOCK_ENABLED = booleanPreferencesKey("sponsorblock_enabled")
    }

    val themeSettings: Flow<ThemeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeConfigName = preferences[PreferencesKeys.THEME_CONFIG] ?: AppThemeConfig.SYSTEM.name
            val themeConfig = try {
                AppThemeConfig.valueOf(themeConfigName)
            } catch (e: IllegalArgumentException) {
                AppThemeConfig.SYSTEM
            }
            val dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true
            ThemeSettings(
                themeConfig = themeConfig,
                dynamicColor = dynamicColor
            )
        }

    val sponsorBlockEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SPONSORBLOCK_ENABLED] ?: true
        }

    suspend fun setThemeConfig(themeConfig: AppThemeConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_CONFIG] = themeConfig.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPONSORBLOCK_ENABLED] = enabled
        }
    }
}
