package com.devson.vedtube.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.vedtube.core.player.PlaybackState
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.core.player.PlayerState
import com.devson.vedtube.core.player.RepeatMode
import com.devson.vedtube.domain.model.VideoStream
import kotlinx.coroutines.delay

/**
 * Overlay playback controls with auto-hide timer, interactive seekbar,
 * quality selector, speed selector, queue management, and fullscreen toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    playerState: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

    // Auto-hide controls after 3.5 seconds if playing
    LaunchedEffect(controlsVisible, playerState.isPlaying) {
        if (controlsVisible && playerState.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Buffering / Loading Indicator in center
        if (playerState.isBuffering || playerState.isResolving || playerState.isPreparing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            playerState.isResolving -> "Resolving stream..."
                            playerState.isPreparing -> "Preparing media..."
                            else -> "Buffering..."
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Floating "Skipped Sponsor" pill
        AnimatedVisibility(
            visible = playerState.sponsorNotification != null,
            enter = fadeIn() + androidx.compose.animation.slideInVertically(),
            exit = fadeOut() + androidx.compose.animation.slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onEvent(PlayerEvent.DismissSponsorNotification) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playerState.sponsorNotification ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay Controls Layer
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                PlayerTopBar(
                    playerState = playerState,
                    onOpenSpeed = { showSpeedSheet = true },
                    onOpenQuality = { showQualitySheet = true },
                    onOpenQueue = { showQueueSheet = true },
                    onOpenSubtitles = { showSubtitleSheet = true },
                    onToggleSubtitles = { onEvent(PlayerEvent.ToggleSubtitles) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Center Action Controls
                PlayerCenterControls(
                    playerState = playerState,
                    onEvent = onEvent,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom Bar (Seekbar, Timers, Fullscreen)
                PlayerBottomBar(
                    playerState = playerState,
                    onEvent = onEvent,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Speed Selection Sheet
        if (showSpeedSheet) {
            SpeedSelectionSheet(
                currentSpeed = playerState.playbackSpeed,
                onSpeedSelected = { speed ->
                    onEvent(PlayerEvent.SetPlaybackSpeed(speed))
                    showSpeedSheet = false
                },
                onDismiss = { showSpeedSheet = false }
            )
        }

        // Quality Selection Sheet
        if (showQualitySheet) {
            QualitySelectionSheet(
                availableStreams = playerState.availableQualities,
                selectedStream = playerState.selectedQuality,
                onQualitySelected = { stream ->
                    onEvent(PlayerEvent.SelectQuality(stream))
                    showQualitySheet = false
                },
                onDismiss = { showQualitySheet = false }
            )
        }

        // Subtitle Selection Sheet
        if (showSubtitleSheet) {
            SubtitleSelectionSheet(
                availableSubtitles = playerState.availableSubtitles,
                selectedSubtitle = playerState.selectedSubtitle,
                areSubtitlesEnabled = playerState.areSubtitlesEnabled,
                onSubtitleSelected = { subtitle ->
                    onEvent(PlayerEvent.SelectSubtitle(subtitle))
                    showSubtitleSheet = false
                },
                onDismiss = { showSubtitleSheet = false }
            )
        }

        // Queue Management Sheet
        if (showQueueSheet) {
            QueueManagementSheet(
                playerState = playerState,
                onEvent = onEvent,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    playerState: PlayerState,
    onOpenSpeed: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onToggleSubtitles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playerState.currentVideo?.title ?: playerState.currentMediaItem?.title ?: "VedTube Player",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!playerState.currentVideo?.uploaderName.isNullOrBlank()) {
                Text(
                    text = playerState.currentVideo?.uploaderName ?: "",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Subtitles CC Toggle
        if (playerState.availableSubtitles.isNotEmpty()) {
            IconButton(
                onClick = {
                    if (playerState.availableSubtitles.size > 1) {
                        onOpenSubtitles()
                    } else {
                        onToggleSubtitles()
                    }
                }
            ) {
                Icon(
                    imageVector = if (playerState.areSubtitlesEnabled) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                    contentDescription = "Closed Captions",
                    tint = if (playerState.areSubtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                )
            }
        }

        IconButton(onClick = onOpenSpeed) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Playback Speed",
                tint = Color.White
            )
        }

        if (playerState.availableQualities.isNotEmpty()) {
            IconButton(onClick = onOpenQuality) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = "Video Quality",
                    tint = Color.White
                )
            }
        }

        IconButton(onClick = onOpenQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Playback Queue",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PlayerCenterControls(
    playerState: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        IconButton(
            onClick = { onEvent(PlayerEvent.Previous) },
            enabled = playerState.hasPrevious || playerState.currentPositionMs > 3000L,
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Track",
                tint = if (playerState.hasPrevious || playerState.currentPositionMs > 3000L) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
        }

        // Seek -10s Button
        IconButton(
            onClick = { onEvent(PlayerEvent.SeekBackward(10_000L)) },
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek -10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Play / Pause / Replay Main Button
        IconButton(
            onClick = {
                if (playerState.isEnded) {
                    onEvent(PlayerEvent.SeekTo(0))
                    onEvent(PlayerEvent.Play)
                } else {
                    onEvent(PlayerEvent.TogglePlayPause)
                }
            },
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = when {
                    playerState.isEnded -> Icons.Default.Replay
                    playerState.isPlaying -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Seek +10s Button
        IconButton(
            onClick = { onEvent(PlayerEvent.SeekForward(10_000L)) },
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek +10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Next Button
        IconButton(
            onClick = { onEvent(PlayerEvent.Next) },
            enabled = playerState.hasNext,
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Track",
                tint = if (playerState.hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PlayerBottomBar(
    playerState: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) dragProgress else playerState.progressFraction

    Column(modifier = modifier) {
        // Seek Bar Slider
        Slider(
            value = effectiveProgress,
            onValueChange = { newProgress ->
                isDragging = true
                dragProgress = newProgress
            },
            onValueChangeFinished = {
                val targetMs = (dragProgress * playerState.durationMs.toFloat()).toLong()
                onEvent(PlayerEvent.SeekTo(targetMs))
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time and Fullscreen Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayPosition = if (isDragging) {
                (dragProgress * playerState.durationMs.toFloat()).toLong()
            } else {
                playerState.currentPositionMs
            }

            Text(
                text = "${formatDuration(displayPosition)} / ${formatDuration(playerState.durationMs)}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Repeat Mode Toggle
                IconButton(
                    onClick = { onEvent(PlayerEvent.ToggleRepeatMode) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.OFF -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Shuffle Toggle
                IconButton(
                    onClick = { onEvent(PlayerEvent.ToggleShuffle) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Fullscreen Toggle
                IconButton(
                    onClick = { onEvent(PlayerEvent.ToggleFullscreen) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            speeds.forEach { speed ->
                val isSelected = (currentSpeed - speed).let { if (it < 0) -it else it } < 0.05f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSpeedSelected(speed) }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelectionSheet(
    availableStreams: List<VideoStream>,
    selectedStream: VideoStream?,
    onQualitySelected: (VideoStream) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                itemsIndexed(availableStreams) { _, stream ->
                    val isSelected = selectedStream?.url == stream.url
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQualitySelected(stream) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${stream.resolution} (${(stream.format ?: "mp4").uppercase()})",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (stream.bitrate > 0) {
                                Text(
                                    text = "${stream.bitrate / 1000} kbps • ${stream.fps} fps",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueManagementSheet(
    playerState: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Queue (${playerState.queue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (playerState.queue.isNotEmpty()) {
                    IconButton(onClick = { onEvent(PlayerEvent.ClearQueue) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Queue",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (playerState.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(playerState.queue) { index, video ->
                        val isCurrent = index == playerState.currentQueueIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { onEvent(PlayerEvent.PlayQueueIndex(index)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = video.uploaderName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onEvent(PlayerEvent.RemoveFromQueue(index)) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleSelectionSheet(
    availableSubtitles: List<com.devson.vedtube.domain.model.SubtitleTrack>,
    selectedSubtitle: com.devson.vedtube.domain.model.SubtitleTrack?,
    areSubtitlesEnabled: Boolean,
    onSubtitleSelected: (com.devson.vedtube.domain.model.SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Subtitles / Captions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Turn Off
            val isOff = !areSubtitlesEnabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOff) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                    .clickable { onSubtitleSelected(null) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Off",
                    color = if (isOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isOff) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (isOff) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Available Subtitles
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(availableSubtitles) { _, sub ->
                    val isSelected = areSubtitlesEnabled && (selectedSubtitle?.languageCode == sub.languageCode || (selectedSubtitle == null && sub == availableSubtitles.firstOrNull()))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { onSubtitleSelected(sub) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.languageName.ifBlank { sub.languageCode },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (sub.isAutoGenerated) {
                                Text(
                                    text = "Auto-generated",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
