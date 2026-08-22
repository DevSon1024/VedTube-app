package com.devson.vedtube.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.player.VedPlayer
import com.devson.vedtube.domain.model.LocalPlaylistDetail
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlistId: String = "",
    val playlistDetail: LocalPlaylistDetail? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    val vedPlayer: VedPlayer,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        val playlistId = savedStateHandle.get<String>("playlistId").orEmpty()
        if (playlistId.isNotBlank()) {
            _uiState.update { it.copy(playlistId = playlistId) }
            playlistRepository.getPlaylistDetail(playlistId).onEach { detail ->
                _uiState.update {
                    it.copy(
                        playlistDetail = detail,
                        isLoading = false,
                        isDeleted = detail == null && !it.isLoading
                    )
                }
            }.launchIn(viewModelScope)
        }
    }

    fun playAll() {
        val videos = _uiState.value.playlistDetail?.videos.orEmpty()
        if (videos.isNotEmpty()) {
            vedPlayer.playVideo(videos.first())
            if (videos.size > 1) {
                vedPlayer.enqueueAll(videos.drop(1))
            }
        }
    }

    fun removeVideo(videoId: String) {
        val playlistId = _uiState.value.playlistId
        if (playlistId.isNotBlank()) {
            viewModelScope.launch(ioDispatcher) {
                playlistRepository.removeVideoFromPlaylist(playlistId, videoId)
            }
        }
    }

    fun deletePlaylist() {
        val playlistId = _uiState.value.playlistId
        if (playlistId.isNotBlank()) {
            viewModelScope.launch(ioDispatcher) {
                playlistRepository.deletePlaylist(playlistId)
                _uiState.update { it.copy(isDeleted = true) }
            }
        }
    }
}
