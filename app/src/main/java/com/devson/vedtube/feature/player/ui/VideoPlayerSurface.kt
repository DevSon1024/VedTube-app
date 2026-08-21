package com.devson.vedtube.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Clean, lifecycle-aware composable surface that hosts the Media3 [PlayerView].
 * Survives recomposition and screen rotation without reallocating ExoPlayer.
 */
@Composable
@OptIn(UnstableApi::class)
fun VideoPlayerSurface(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    useController: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    val context = LocalContext.current

    val playerView = remember {
        PlayerView(context).apply {
            this.player = exoPlayer
            this.useController = useController
            this.resizeMode = resizeMode
            this.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        }
    }

    DisposableEffect(exoPlayer) {
        playerView.player = exoPlayer
        onDispose {
            playerView.player = null
        }
    }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
                view.resizeMode = resizeMode
                view.useController = useController
            }
        )
    }
}
