package com.devson.vedtube.feature.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.player.PlayerEvent
import com.devson.vedtube.core.player.PlayerState
import com.devson.vedtube.core.player.VedPlayer
import com.devson.vedtube.domain.model.AppError
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails
import com.devson.vedtube.domain.provider.MediaProvider
import com.devson.vedtube.domain.repository.SubscriptionRepository
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VideoDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mediaProvider: MediaProvider,
    val vedPlayer: VedPlayer,
    private val subscriptionRepository: SubscriptionRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailsUiState())
    val uiState: StateFlow<VideoDetailsUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = vedPlayer.playerState
    private var subscriptionObserveJob: Job? = null

    init {
        // Observe watch history progress for related video thumbnails
        watchHistoryRepository.getRecentHistory().onEach { historyList ->
            val progressMap = historyList.associate { it.videoId to it.progressFraction }
            _uiState.update { it.copy(watchProgressMap = progressMap) }
        }.launchIn(viewModelScope)

        val initialVideoId = savedStateHandle.get<String>("videoId")
        if (!initialVideoId.isNullOrBlank()) {
            loadVideo(initialVideoId)
        }
    }

    fun loadVideo(videoId: String, initialVideo: Video? = null) {
        _uiState.update {
            it.copy(
                videoId = videoId,
                isLoadingDetails = true,
                error = null,
                isDescriptionExpanded = false
            )
        }

        // Trigger playback on player if it's not already playing this exact video
        val currentPlayingVideo = vedPlayer.playerState.value.currentVideo
        if (currentPlayingVideo?.id != videoId) {
            if (initialVideo != null) {
                vedPlayer.playVideo(initialVideo)
            } else {
                vedPlayer.playVideoId(
                    videoId = videoId,
                    title = "Video ($videoId)"
                )
            }
        }

        // Fetch rich video metadata & related videos asynchronously
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                mediaProvider.getVideoDetails(videoId)
            }

            result.onSuccess { details ->
                _uiState.update {
                    it.copy(
                        details = details,
                        isLoadingDetails = false,
                        error = null
                    )
                }
                observeSubscription(details.uploaderId ?: details.uploaderName)
            }.onFailure { err ->
                val fallbackDetails = initialVideo?.let { v ->
                    VideoDetails(
                        id = v.id,
                        title = v.title,
                        uploaderName = v.uploaderName,
                        uploaderAvatarUrl = v.uploaderAvatarUrl,
                        thumbnailUrl = v.thumbnailUrl,
                        durationSeconds = v.durationSeconds,
                        viewCount = v.viewCount,
                        uploadDate = v.uploadDate
                    )
                } ?: currentPlayingVideo?.let { v ->
                    VideoDetails(
                        id = v.id,
                        title = v.title,
                        uploaderName = v.uploaderName,
                        uploaderAvatarUrl = v.uploaderAvatarUrl,
                        thumbnailUrl = v.thumbnailUrl,
                        durationSeconds = v.durationSeconds,
                        viewCount = v.viewCount,
                        uploadDate = v.uploadDate
                    )
                }

                _uiState.update {
                    it.copy(
                        details = fallbackDetails,
                        isLoadingDetails = false,
                        error = if (fallbackDetails == null) {
                            (err as? AppError) ?: AppError.Unknown(err.message ?: "Failed to load video details", err)
                        } else null
                    )
                }

                if (fallbackDetails != null) {
                    observeSubscription(fallbackDetails.uploaderId ?: fallbackDetails.uploaderName)
                }
            }
        }
    }

    private fun observeSubscription(channelId: String) {
        subscriptionObserveJob?.cancel()
        subscriptionObserveJob = subscriptionRepository.isSubscribed(channelId).onEach { isSub ->
            _uiState.update { it.copy(isSubscribed = isSub) }
        }.launchIn(viewModelScope)
    }

    fun toggleSubscription() {
        val details = _uiState.value.details ?: return
        val channelId = details.uploaderId ?: details.uploaderName
        viewModelScope.launch(ioDispatcher) {
            val isCurrentlySubscribed = subscriptionRepository.isSubscribedSync(channelId)
            if (isCurrentlySubscribed) {
                subscriptionRepository.unsubscribe(channelId)
            } else {
                subscriptionRepository.subscribe(
                    channelId = channelId,
                    channelName = details.uploaderName,
                    avatarUrl = details.uploaderAvatarUrl ?: ""
                )
            }
        }
    }

    fun onRelatedVideoClick(video: Video) {
        loadVideo(videoId = video.id, initialVideo = video)
    }

    fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun onPlayerEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }
}
