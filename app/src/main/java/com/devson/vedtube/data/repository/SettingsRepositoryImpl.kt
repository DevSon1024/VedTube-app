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
}
