package com.devson.vedtube.feature.video.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedtube.core.player.PlaybackState
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails
import com.devson.vedtube.feature.common.FormatUtils
import com.devson.vedtube.feature.common.VideoCard
import com.devson.vedtube.feature.common.VideoCardShimmer
import com.devson.vedtube.feature.player.ui.PlayerControls
import com.devson.vedtube.feature.player.ui.VideoPlayerSurface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import com.devson.vedtube.feature.video.VideoDetailsViewModel

@Composable
fun VideoDetailsScreen(
    viewModel: VideoDetailsViewModel,
    onBackClick: () -> Unit,
    isInPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = context as? Activity

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // In PiP mode, display ONLY the clean video surface filling the entire PiP window
    if (isInPipMode) {
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

    // Intercept back navigation when in fullscreen or navigating back
    BackHandler {
        if (playerState.isFullscreen) {
            viewModel.onPlayerEvent(PlayerEvent.SetFullscreen(false))
        } else {
            onBackClick()
        }
    }

    // Sync fullscreen state with screen orientation
    LaunchedEffect(playerState.isFullscreen) {
        activity?.requestedOrientation = if (playerState.isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape || playerState.isFullscreen) {
            // Immersive Fullscreen Video Player in Landscape
            Box(modifier = Modifier.fillMaxSize()) {
                VideoPlayerSurface(
                    exoPlayer = viewModel.vedPlayer.exoPlayer,
                    modifier = Modifier.fillMaxSize()
                )
                PlayerControls(
                    playerState = playerState,
                    onEvent = viewModel::onPlayerEvent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Portrait Layout: 16:9 Player on Top + Scrollable Video Details & Related Videos Below
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Video Player Container with Back Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayerSurface(
                        exoPlayer = viewModel.vedPlayer.exoPlayer,
                        modifier = Modifier.fillMaxSize()
                    )
                    PlayerControls(
                        playerState = playerState,
                        onEvent = viewModel::onPlayerEvent,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top navigation back button
                    IconButton(
                        onClick = {
                            if (playerState.isFullscreen) {
                                viewModel.onPlayerEvent(PlayerEvent.SetFullscreen(false))
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                // Error Banner (if any)
                if (playerState.playbackState is PlaybackState.Error) {
                    val errorState = playerState.playbackState as PlaybackState.Error
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorState.error.message ?: "Playback error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { viewModel.onPlayerEvent(PlayerEvent.Retry) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Scrollable Content: Metadata, Channel, Description, Related Videos
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Video Title and Metrics
                    item {
                        val currentTitle = uiState.details?.title
                            ?: playerState.currentVideo?.title
                            ?: "Loading Video..."

                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val viewsDateText = buildString {
                            val viewCount = uiState.details?.viewCount
                                ?: playerState.currentVideo?.viewCount ?: 0L
                            if (viewCount > 0) {
                                append(FormatUtils.formatViewCount(viewCount))
                            }
                            val date = uiState.details?.uploadDate
                                ?: playerState.currentVideo?.uploadDate
                            if (!date.isNullOrBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(date)
                            }
                        }

                        if (viewsDateText.isNotBlank()) {
                            Text(
                                text = viewsDateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Channel Information Row
                    item {
                        val channelName = uiState.details?.uploaderName
                            ?: playerState.currentVideo?.uploaderName ?: "Creator"
                        val avatarUrl = uiState.details?.uploaderAvatarUrl
                            ?: playerState.currentVideo?.uploaderAvatarUrl

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = channelName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = channelName.firstOrNull()?.uppercase() ?: "C",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = channelName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subscribers = FormatUtils.formatSubscriberCount(uiState.details?.subscriberCount)
                                    if (subscribers.isNotBlank()) {
                                        Text(
                                            text = subscribers,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (uiState.isSubscribed) {
                                Button(
                                    onClick = { viewModel.toggleSubscription() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Subscribed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleSubscription() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Subscribe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Expandable Description Box
                    item {
                        val descriptionText = uiState.details?.description ?: ""
                        if (descriptionText.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.toggleDescription() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Description",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (uiState.isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (uiState.isDescriptionExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = descriptionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (uiState.isDescriptionExpanded) Int.MAX_VALUE else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 19.sp
                                    )

                                    if (!uiState.isDescriptionExpanded) {
                                        Text(
                                            text = "...more",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Related Videos Header
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Related Videos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Related Videos List
                    val related = uiState.details?.relatedVideos.orEmpty()
                    if (related.isNotEmpty()) {
                        items(
                            items = related,
                            key = { it.id }
                        ) { relatedVideo ->
                            VideoCard(
                                video = relatedVideo,
                                watchProgressFraction = uiState.watchProgressMap[relatedVideo.id],
                                onClick = { viewModel.onRelatedVideoClick(relatedVideo) }
                            )
                        }
                    } else if (uiState.isLoadingDetails) {
                        items(3) {
                            VideoCardShimmer()
                        }
                    } else {
                        item {
                            Text(
                                text = "No related videos available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
