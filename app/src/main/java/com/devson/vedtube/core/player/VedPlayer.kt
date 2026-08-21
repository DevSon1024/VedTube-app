package com.devson.vedtube.core.player

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * Primary player interface managing ExoPlayer lifecycle, reactive state observation,
 * and command dispatching.
 */
interface VedPlayer : PlayerController {
    val playerState: StateFlow<PlayerState>
    val exoPlayer: ExoPlayer
    val queueManager: QueueManager
}
