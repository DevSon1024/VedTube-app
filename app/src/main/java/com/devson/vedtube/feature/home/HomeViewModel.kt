package com.devson.vedtube.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.core.player.VedPlayer
import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl
import com.devson.vedtube.data.provider.youtube.url.YoutubeUrlParser
import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.ThemeSettings
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.provider.MediaProvider
import com.devson.vedtube.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

private data class SearchState(
    val query: String = "",
    val active: Boolean = false,
    val results: List<Video> = emptyList(),
    val isSearching: Boolean = false
)

private data class FeedState(
    val feed: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaProvider: MediaProvider,
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: com.devson.vedtube.domain.repository.SearchHistoryRepository,
    private val appInfoDao: AppInfoDao,
    private val okHttpClient: OkHttpClient,
    val vedPlayer: VedPlayer,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    private val _feedVideos = MutableStateFlow<List<Video>>(emptyList())
    private val _isLoadingFeed = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _error = MutableStateFlow<AppError?>(null)

    private val _infraState = MutableStateFlow(Triple(true, true, true))

    private var searchJob: Job? = null

    private val _searchQueryAndActive = combine(_searchQuery, _isSearchActive) { query, active ->
        Pair(query, active)
    }

    private val _searchResultsAndSearching = combine(_searchResults, _isSearching) { results, searching ->
        Pair(results, searching)
    }

    private val _searchState = combine(
        _searchQueryAndActive,
        _searchResultsAndSearching
    ) { queryAndActive, resultsAndSearching ->
        SearchState(
            query = queryAndActive.first,
            active = queryAndActive.second,
            results = resultsAndSearching.first,
            isSearching = resultsAndSearching.second
        )
    }

    private val _feedLoadingPair = combine(_feedVideos, _isLoadingFeed) { feed, loading ->
        Pair(feed, loading)
    }

    private val _feedState = combine(
        _feedLoadingPair,
        _error
    ) { feedAndLoading, error ->
        FeedState(
            feed = feedAndLoading.first,
            isLoading = feedAndLoading.second,
            error = error
        )
    }

    private val _settingsAndInfra = combine(
        settingsRepository.themeSettings,
        settingsRepository.sponsorBlockEnabled,
        _infraState
    ) { theme, sponsorBlock, infra ->
        Triple(theme, sponsorBlock, infra)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _settingsAndInfra,
        _searchState,
        _feedState,
        searchHistoryRepository.getRecentQueries()
    ) { settingsAndInfra, searchState, feedState, recentSearches ->
        val themeSettings = settingsAndInfra.first
        val sponsorBlockEnabled = settingsAndInfra.second
        val infra = settingsAndInfra.third
        HomeUiState(
            searchQuery = searchState.query,
            isSearchActive = searchState.active,
            searchResults = searchState.results,
            feedVideos = feedState.feed,
            isLoadingFeed = feedState.isLoading,
            isSearching = searchState.isSearching,
            error = feedState.error,
            themeSettings = themeSettings,
            isSponsorBlockEnabled = sponsorBlockEnabled,
            recentSearches = recentSearches,
            isDatabaseReady = infra.first,
            isNetworkReady = infra.second,
            isPlayerReady = infra.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoadingFeed = true)
    )

    private var currentRegion: String = "IN"

    init {
        verifyInfrastructure()
        viewModelScope.launch {
            settingsRepository.contentRegion.collect { region ->
                currentRegion = region
                loadFeed(region)
            }
        }
    }

    fun loadInitialFeed() {
        loadFeed(currentRegion)
    }

    fun loadFeed(region: String? = currentRegion) {
        viewModelScope.launch {
            _isLoadingFeed.value = true
            _error.value = null
            val result = withContext(ioDispatcher) {
                mediaProvider.getTrendingFeed(region)
            }
            _isLoadingFeed.value = false
            result.onSuccess { paged ->
                if (paged.items.isNotEmpty()) {
                    _feedVideos.value = paged.items
                } else {
                    loadFallbackFeed()
                }
            }.onFailure { err ->
                _error.value = (err as? AppError) ?: AppError.Unknown(err.message ?: "Failed to load feed", err)
                loadFallbackFeed()
            }
        }
    }

    private suspend fun loadFallbackFeed() {
        if (_feedVideos.value.isEmpty()) {
            _feedVideos.value = listOf(
                Video(
                    id = "aqz-KE-bpKQ",
                    title = "Big Buck Bunny 60fps 4K",
                    uploaderName = "Blender Foundation",
                    thumbnailUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800",
                    durationSeconds = 596,
                    viewCount = 12500000,
                    uploadDate = "May 2008"
                ),
                Video(
                    id = "dQw4w9WgXcQ",
                    title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
                    uploaderName = "Rick Astley",
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                    durationSeconds = 213,
                    viewCount = 1450000000,
                    uploadDate = "Oct 2009"
                ),
                Video(
                    id = "L_LUpnjgPso",
                    title = "Elephants Dream (Open Source Cinema)",
                    uploaderName = "Orange Open Movie",
                    thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800",
                    durationSeconds = 654,
                    viewCount = 4300000,
                    uploadDate = "Mar 2006"
                )
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(600)
            executeSearch(query)
        }
    }

    fun onSearchSubmitted(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            _searchQuery.value = trimmed
            viewModelScope.launch {
                searchHistoryRepository.saveQuery(trimmed)
                executeSearch(trimmed)
            }
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    private suspend fun executeSearch(query: String) {
        _isSearching.value = true
        _error.value = null
        val result = withContext(ioDispatcher) {
            mediaProvider.search(query)
        }
        _isSearching.value = false
        result.onSuccess { paged ->
            _searchResults.value = paged.items
        }.onFailure { err ->
            _error.value = (err as? AppError) ?: AppError.Unknown(err.message ?: "Search failed", err)
        }
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active && _searchQuery.value.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        _error.value = null
    }

    fun onPlayerEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }

    fun handleIncomingIntent(urlOrText: String) {
        viewModelScope.launch {
            val parsed = withContext(ioDispatcher) {
                YoutubeUrlParser.parse(urlOrText)
            }

            when (parsed) {
                is ParsedMediaUrl.Video -> {
                    vedPlayer.playVideoId(
                        videoId = parsed.videoId,
                        title = "Shared Video (${parsed.videoId})"
                    )
                }
                else -> { /* Handle Playlist or Channel */ }
            }
        }
    }

    fun toggleTheme(config: AppThemeConfig) {
        viewModelScope.launch {
            settingsRepository.setThemeConfig(config)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun toggleSponsorBlock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSponsorBlockEnabled(enabled)
        }
    }

    private fun verifyInfrastructure() {
        viewModelScope.launch(ioDispatcher) {
            val dbReady = try {
                appInfoDao.insertInfo(
                    AppInfoEntity(
                        key = "init_check",
                        value = System.currentTimeMillis().toString()
                    )
                )
                val readBack = appInfoDao.getInfo("init_check")
                readBack != null
            } catch (e: Exception) {
                false
            }

            val netReady = try {
                okHttpClient.connectionPool.connectionCount() >= 0
            } catch (e: Exception) {
                false
            }

            val playerReady = true

            _infraState.value = Triple(dbReady, netReady, playerReady)
        }
    }
}
