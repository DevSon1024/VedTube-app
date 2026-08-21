package com.devson.vedtube.domain.model

import com.devson.vedtube.core.datastore.model.AppThemeConfig

data class ThemeSettings(
    val themeConfig: AppThemeConfig = AppThemeConfig.SYSTEM,
    val dynamicColor: Boolean = true
)
