@file:OptIn(ExperimentalMaterial3Api::class)

package com.devson.vedtube.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedtube.core.player.PlayerEvent
import androidx.activity.compose.BackHandler
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.feature.common.EmptySearchState
import com.devson.vedtube.feature.common.ErrorState
import com.devson.vedtube.feature.common.VideoCard
import com.devson.vedtube.feature.common.VideoCardShimmer

import androidx.compose.ui.graphics.Color
import com.devson.vedtube.feature.player.ui.VideoPlayerSurface

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Video) -> Unit,
    isInPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.vedPlayer.playerState.collectAsState()

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
    BackHandler(enabled = uiState.isSearchActive || uiState.searchQuery.isNotBlank()) {
        if (uiState.isSearchActive) {
            viewModel.setSearchActive(false)
        } else if (uiState.searchQuery.isNotBlank()) {
            viewModel.clearSearch()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearch = { viewModel.onSearchSubmitted(it) },
                active = uiState.isSearchActive,
                onActiveChange = { viewModel.setSearchActive(it) },
                onClearQuery = { viewModel.clearSearch() }
            )
        },
        bottomBar = {
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

                // Main Content List
                when {
                    uiState.isLoading && uiState.displayVideos.isEmpty() -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(4) {
                                VideoCardShimmer()
                            }
                        }
                    }

                    uiState.error != null && uiState.displayVideos.isEmpty() -> {
                        ErrorState(
                            error = uiState.error,
                            onRetry = {
                                if (uiState.isSearchMode) {
                                    viewModel.onSearchSubmitted(uiState.searchQuery)
                                } else {
                                    viewModel.loadInitialFeed()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    uiState.isSearchMode && !uiState.isLoading && uiState.searchResults.isEmpty() -> {
                        EmptySearchState(
                            query = uiState.searchQuery,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.displayVideos,
                                key = { it.id }
                            ) { video ->
                                VideoCard(
                                    video = video,
                                    onClick = { onVideoClick(video) }
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
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            placeholder = { Text("Search VedTube...") },
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
                            contentDescription = "Back"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear text"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search suggestions or quick categories inside active search view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf("Trending Videos", "Kotlin Coroutines", "Jetpack Compose", "4K Nature", "Lo-Fi Beats").forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onQueryChange(suggestion)
                                onSearch(suggestion)
                                onActiveChange(false)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge
                        )
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
    val categories = listOf("All", "Trending", "Music", "Gaming", "Tech", "News", "Podcasts", "Learning")
    var selectedCategory by remember { mutableStateOf("All") }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Channel
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

            // Play / Pause Toggle
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Close
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close player",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
