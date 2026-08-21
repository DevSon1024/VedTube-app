package com.devson.vedtube.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtube.R
import com.devson.vedtube.core.datastore.model.AppThemeConfig
import com.devson.vedtube.data.provider.youtube.url.ParsedMediaUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputUrl by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phase 1 — URL & Deep-Link Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "URL & Deep-Link Engine Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Supported formats: Videos, Shorts, Live, Embeds, Playlists, Channels, Handles (@), timestamps, and shared text intents.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Ingested Deep Link / Share Intent Card
            if (uiState.parsedMediaUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Incoming URL / Deep-Link Parsed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        uiState.rawIncomingUrl?.let { raw ->
                            Text(
                                text = "Raw: $raw",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        when (val parsed = uiState.parsedMediaUrl) {
                            is ParsedMediaUrl.Video -> {
                                Text(
                                    text = "Type: VIDEO\nVideo ID: ${parsed.videoId}" +
                                            (parsed.playlistId?.let { "\nPlaylist ID: $it" } ?: "") +
                                            (parsed.timestampMs?.let { "\nTimestamp: ${it / 1000}s (${it}ms)" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            is ParsedMediaUrl.Playlist -> {
                                Text(
                                    text = "Type: PLAYLIST\nPlaylist ID: ${parsed.playlistId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            is ParsedMediaUrl.Channel -> {
                                val channelDetails = when (parsed) {
                                    is ParsedMediaUrl.Channel.Id -> "ID: ${parsed.channelId}"
                                    is ParsedMediaUrl.Channel.Handle -> "Handle: ${parsed.handle}"
                                    is ParsedMediaUrl.Channel.CustomUrl -> "Custom: ${parsed.customUrl}"
                                    is ParsedMediaUrl.Channel.User -> "User: ${parsed.username}"
                                }
                                Text(
                                    text = "Type: CHANNEL\n$channelDetails",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            ParsedMediaUrl.Unknown -> {
                                Text(
                                    text = "Type: UNKNOWN (Invalid or Unsupported URL)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }

            // Interactive URL Tester Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Interactive URL Parser Tester",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Enter YouTube URL or shared text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.handleIncomingIntent(inputUrl) },
                            enabled = inputUrl.isNotBlank()
                        ) {
                            Text("Parse URL")
                        }

                        Button(
                            onClick = {
                                inputUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=90s"
                                viewModel.handleIncomingIntent(inputUrl)
                            }
                        ) {
                            Text("Sample Video")
                        }
                    }
                }
            }

            // Theme Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.themeSettings.themeConfig == AppThemeConfig.SYSTEM,
                            onClick = { viewModel.setThemeConfig(AppThemeConfig.SYSTEM) },
                            label = { Text("System") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                        FilterChip(
                            selected = uiState.themeSettings.themeConfig == AppThemeConfig.LIGHT,
                            onClick = { viewModel.setThemeConfig(AppThemeConfig.LIGHT) },
                            label = { Text("Light") },
                            leadingIcon = {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        FilterChip(
                            selected = uiState.themeSettings.themeConfig == AppThemeConfig.DARK,
                            onClick = { viewModel.setThemeConfig(AppThemeConfig.DARK) },
                            label = { Text("Dark") },
                            leadingIcon = {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Dynamic Color",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Available on Android 12+",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Switch(
                            checked = uiState.themeSettings.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                }
            }

            // Infrastructure Checklist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Infrastructure Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StatusRow(
                        icon = Icons.Default.Storage,
                        title = "Room Database",
                        status = if (uiState.isDatabaseReady) "Connected (vedtube.db)" else "Initializing..."
                    )
                    StatusRow(
                        icon = Icons.Default.Wifi,
                        title = "OkHttp Client",
                        status = if (uiState.isNetworkReady) "Configured (Logging & Cache active)" else "Initializing..."
                    )
                    StatusRow(
                        icon = Icons.Default.PlayCircle,
                        title = "Media3 ExoPlayer",
                        status = if (uiState.isPlayerReady) "Compiled & Initialized" else "Initializing..."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
