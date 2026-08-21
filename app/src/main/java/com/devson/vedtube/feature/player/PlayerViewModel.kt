package com.devson.vedtube.feature.player

import androidx.lifecycle.ViewModel
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.core.player.PlayerState
import com.devson.vedtube.core.player.VedPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel bridging UI components to the singleton [VedPlayer] lifecycle.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val vedPlayer: VedPlayer
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = vedPlayer.playerState
    val exoPlayer = vedPlayer.exoPlayer

    fun onEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }

    override fun onCleared() {
        super.onCleared()
        // Player lifecycle is managed as a Singleton across navigation,
        // so we don't release it here.
    }
}
