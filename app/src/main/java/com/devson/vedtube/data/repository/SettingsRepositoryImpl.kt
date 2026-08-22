package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.datastore.UserPreferencesDataStore
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    override val themeSettings: Flow<ThemeSettings>
        get() = userPreferencesDataStore.themeSettings.flowOn(ioDispatcher)

    override val sponsorBlockEnabled: Flow<Boolean>
        get() = userPreferencesDataStore.sponsorBlockEnabled.flowOn(ioDispatcher)

    override val skipIntervalSeconds: Flow<Int>
        get() = userPreferencesDataStore.skipIntervalSeconds.flowOn(ioDispatcher)

    override val distractionFreeMode: Flow<Boolean>
        get() = userPreferencesDataStore.distractionFreeMode.flowOn(ioDispatcher)

    override val activeProfileId: Flow<String>
        get() = userPreferencesDataStore.activeProfileId.flowOn(ioDispatcher)

    override suspend fun setThemeConfig(themeConfig: AppThemeConfig) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setThemeConfig(themeConfig)
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setDynamicColor(enabled)
        }
    }

    override suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setSponsorBlockEnabled(enabled)
        }
    }

    override suspend fun setSkipIntervalSeconds(seconds: Int) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setSkipIntervalSeconds(seconds)
        }
    }

    override suspend fun setDistractionFreeMode(enabled: Boolean) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setDistractionFreeMode(enabled)
        }
    }

    override suspend fun setActiveProfileId(profileId: String) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setActiveProfileId(profileId)
        }
    }
}
