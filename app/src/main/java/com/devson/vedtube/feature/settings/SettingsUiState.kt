package com.devson.vedtube.feature.settings

import com.devson.vedtube.domain.model.BackupSummary
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.UserProfile

data class CountryRegion(
    val code: String,
    val name: String,
    val flag: String
)

data class AppLanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

data class SettingsUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isSponsorBlockEnabled: Boolean = true,
    val skipIntervalSeconds: Int = 10,
    val isDistractionFreeMode: Boolean = false,
    val contentRegion: String = "IN",
    val appLanguage: String = "system",
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
) {
    companion object {
        val SUPPORTED_REGIONS = listOf(
            CountryRegion("IN", "India", "🇮🇳"),
            CountryRegion("US", "United States", "🇺🇸"),
            CountryRegion("GB", "United Kingdom", "🇬🇧"),
            CountryRegion("CA", "Canada", "🇨🇦"),
            CountryRegion("AU", "Australia", "🇦🇺"),
            CountryRegion("DE", "Germany", "🇩🇪"),
            CountryRegion("FR", "France", "🇫🇷"),
            CountryRegion("JP", "Japan", "🇯🇵"),
            CountryRegion("KR", "South Korea", "🇰🇷"),
            CountryRegion("BR", "Brazil", "🇧🇷")
        )

        val SUPPORTED_LANGUAGES = listOf(
            AppLanguageOption("system", "System Default", "सिस्टम डिफ़ॉल्ट"),
            AppLanguageOption("en", "English", "English"),
            AppLanguageOption("hi", "Hindi", "हिन्दी")
        )
    }
}
