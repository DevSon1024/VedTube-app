@file:OptIn(ExperimentalMaterial3Api::class)

package com.devson.vedtube.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.devson.vedtube.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.feature.common.EmptySearchState
import com.devson.vedtube.feature.common.ErrorState
import com.devson.vedtube.feature.common.VideoCard
import com.devson.vedtube.feature.common.VideoCardShimmer
import com.devson.vedtube.feature.library.LibraryViewModel
import com.devson.vedtube.feature.library.ui.LibraryScreen
import com.devson.vedtube.feature.player.ui.VideoPlayerSurface

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Video) -> Unit,
    isInPipMode: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.vedPlayer.playerState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // In PiP mode, display ONLY pure video player if active
    if (isInPipMode && playerState.currentVideo != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerSurface(
                exoPlayer = viewModel.vedPlayer.exoPlayer,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // Handle system back navigation gracefully on HomeScreen
    BackHandler(enabled = selectedTab != 0 || uiState.isSearchActive || uiState.searchQuery.isNotBlank()) {
        if (uiState.isSearchActive) {
            viewModel.setSearchActive(false)
        } else if (uiState.searchQuery.isNotBlank()) {
            viewModel.clearSearch()
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (selectedTab == 0) {
                HomeSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { viewModel.onSearchSubmitted(it) },
                    active = uiState.isSearchActive,
                    onActiveChange = { viewModel.setSearchActive(it) },
                    onClearQuery = { viewModel.clearSearch() },
                    recentSearches = uiState.recentSearches,
                    onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
                    onClearSearchHistory = { viewModel.clearSearchHistory() },
                    onSettingsClick = onSettingsClick
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent Mini Player docked above navigation
                AnimatedVisibility(
                    visible = playerState.currentVideo != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    playerState.currentVideo?.let { currentVideo ->
                        HomeMiniPlayer(
                            video = currentVideo,
                            isPlaying = playerState.isPlaying,
                            onPlayPauseClick = { viewModel.onPlayerEvent(PlayerEvent.TogglePlayPause) },
                            onCloseClick = { viewModel.onPlayerEvent(PlayerEvent.Stop) },
                            onClick = { onVideoClick(currentVideo) }
                        )
                    }
                }

                // Material 3 Bottom Navigation Bar
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = stringResource(R.string.nav_library)) },
                        label = { Text(stringResource(R.string.nav_library)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Filter Chips (when not in full search mode)
                    if (!uiState.isSearchActive) {
                        CategoryFilterRow(
                            onCategorySelected = { category ->
                                if (category == "All") {
                                    viewModel.clearSearch()
                                    viewModel.loadInitialFeed()
                                } else {
                                    viewModel.onSearchQueryChanged(category)
                                    viewModel.onSearchSubmitted(category)
                                }
                            }
                        )
                    }

                    // Main Content Feed or Search Results
                    when {
                        uiState.isLoading && uiState.displayVideos.isEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                items(5) {
                                    VideoCardShimmer()
                                }
                            }
                        }

                        uiState.isSearchActive && uiState.displayVideos.isEmpty() && !uiState.isLoading -> {
                            EmptySearchState(
                                query = uiState.searchQuery,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        uiState.error != null && uiState.displayVideos.isEmpty() -> {
                            ErrorState(
                                error = uiState.error!!,
                                onRetry = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        viewModel.onSearchSubmitted(uiState.searchQuery)
                                    } else {
                                        viewModel.loadInitialFeed()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.displayVideos,
                                    key = { index, video -> "home_${video.id}_$index" }
                                ) { _, video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { onVideoClick(video) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onVideoClick = onVideoClick,
                    onPlaylistClick = onPlaylistClick,
                    onSettingsClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    recentSearches: List<com.devson.vedtube.domain.model.SearchHistoryItem> = emptyList(),
    onDeleteSearchQuery: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {
                onSearch(it)
                onActiveChange(false)
            },
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                if (active || query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onActiveChange(false)
                            onClearQuery()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_placeholder)
                    )
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    } else if (!active) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (query.isNotBlank()) {
                // Live query item
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSearch(query)
                                    onActiveChange(false)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (recentSearches.isNotEmpty()) {
                // Search History List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.recent_searches),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onClearSearchHistory) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.clear_all))
                            }
                        }
                    }

                    items(
                        items = recentSearches,
                        key = { "search_${it.query}" }
                    ) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQueryChange(item.query)
                                    onSearch(item.query)
                                    onActiveChange(false)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = item.query,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSearchQuery(item.query) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete search query",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Music", "Gaming", "Podcasts", "News", "Technology", "Live", "Coding")
    var selectedCategory by remember { mutableStateOf("All") }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = {
                    selectedCategory = category
                    onCategorySelected(category)
                },
                label = {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun HomeMiniPlayer(
    video: Video,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Channel
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Pause Action Button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Close Button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close player",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
