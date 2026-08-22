@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.devson.vedtube.core.player.model

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Aspect ratio resize / scaling modes for Media3 PlayerView.
 */
@OptIn(UnstableApi::class)
enum class VideoResizeMode(val displayName: String, val exoResizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

    fun next(): VideoResizeMode = when (this) {
        FIT -> FILL
        FILL -> ZOOM
        ZOOM -> FIT
    }
}
