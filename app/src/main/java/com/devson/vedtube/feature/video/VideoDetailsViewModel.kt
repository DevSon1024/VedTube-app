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
import com.devson.vedtube.domain.model.PlaybackPreferences
import com.devson.vedtube.domain.model.Video
import com.devson.vedtube.domain.model.VideoDetails
import com.devson.vedtube.domain.model.VideoStream
import com.devson.vedtube.domain.provider.MediaProvider
import com.devson.vedtube.domain.repository.DownloadRepository
import com.devson.vedtube.domain.repository.SubscriptionRepository
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import com.devson.vedtube.domain.resolver.PlaybackResolver
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
    private val downloadRepository: DownloadRepository,
    private val playbackResolver: PlaybackResolver,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailsUiState())
    val uiState: StateFlow<VideoDetailsUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = vedPlayer.playerState
    private var subscriptionObserveJob: Job? = null
    private var downloadObserveJob: Job? = null

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
                isDescriptionExpanded = false,
                comments = emptyList(),
                commentsNextPageToken = null,
                isLoadingComments = false,
                isLoadingMoreComments = false,
                commentsError = null,
                totalCommentsCount = null,
                isCommentsSheetVisible = false
            )
        }

        observeDownloadStatus(videoId)

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
                loadComments(videoId)
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

    private fun observeDownloadStatus(videoId: String) {
        downloadObserveJob?.cancel()
        downloadObserveJob = downloadRepository.getDownload(videoId).onEach { download ->
            _uiState.update { it.copy(downloadItem = download) }
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

    fun onDownloadClick() {
        val videoId = _uiState.value.videoId
        if (videoId.isBlank()) return

        val currentDownload = _uiState.value.downloadItem
        if (currentDownload != null && currentDownload.isCompleted) {
            return
        }

        _uiState.update { it.copy(isLoadingDownloadStreams = true) }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                playbackResolver.resolve(videoId, PlaybackPreferences())
            }

            result.onSuccess { source ->
                // Filter progressive streams with direct audio+video (or all streams available)
                val progressiveStreams = source.streams.ifEmpty {
                    // Fallback stream if list is empty
                    listOf(
                        VideoStream(
                            url = "https://www.youtube.com/watch?v=$videoId",
                            resolution = "720p",
                            width = 1280,
                            height = 720,
                            bitrate = 1000000,
                            fps = 30,
                            format = "mp4"
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoadingDownloadStreams = false,
                        availableDownloadStreams = progressiveStreams,
                        isDownloadQualitySheetVisible = true
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingDownloadStreams = false,
                        error = AppError.ContentUnavailable("Could not fetch download stream options")
                    )
                }
            }
        }
    }

    fun onSelectDownloadQuality(stream: VideoStream) {
        val video = _uiState.value.details?.toVideo()
            ?: Video(
                id = _uiState.value.videoId,
                title = _uiState.value.details?.title ?: "Video",
                uploaderName = _uiState.value.details?.uploaderName ?: "Creator"
            )

        _uiState.update { it.copy(isDownloadQualitySheetVisible = false) }

        viewModelScope.launch(ioDispatcher) {
            downloadRepository.startDownload(video, stream)
        }
    }

    fun dismissDownloadQualitySheet() {
        _uiState.update { it.copy(isDownloadQualitySheetVisible = false) }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch(ioDispatcher) {
            downloadRepository.deleteDownload(videoId)
        }
    }

    fun onRelatedVideoClick(video: Video) {
        loadVideo(videoId = video.id, initialVideo = video)
    }

    fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun openComments() {
        _uiState.update { it.copy(isCommentsSheetVisible = true) }
        if (_uiState.value.comments.isEmpty() && !_uiState.value.isLoadingComments) {
            val videoId = _uiState.value.videoId
            if (videoId.isNotBlank()) {
                loadComments(videoId)
            }
        }
    }

    fun dismissComments() {
        _uiState.update { it.copy(isCommentsSheetVisible = false) }
    }

    fun loadComments(videoId: String) {
        _uiState.update {
            it.copy(
                isLoadingComments = true,
                commentsError = null
            )
        }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                mediaProvider.getComments(videoId, pageToken = null)
            }

            result.onSuccess { pagedComments ->
                _uiState.update {
                    it.copy(
                        comments = pagedComments.items,
                        commentsNextPageToken = pagedComments.nextPageToken,
                        totalCommentsCount = pagedComments.totalResults,
                        isLoadingComments = false,
                        commentsError = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        commentsError = err.message ?: "Failed to load comments"
                    )
                }
            }
        }
    }

    fun loadMoreComments() {
        val state = _uiState.value
        val nextToken = state.commentsNextPageToken
        if (nextToken.isNullOrBlank() || state.isLoadingMoreComments || state.isLoadingComments) {
            return
        }

        _uiState.update { it.copy(isLoadingMoreComments = true) }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                mediaProvider.getComments(state.videoId, pageToken = nextToken)
            }

            result.onSuccess { pagedComments ->
                _uiState.update { current ->
                    // Prevent duplicate comments if API returns repeated items
                    val existingIds = current.comments.map { it.id }.toSet()
                    val newUniqueItems = pagedComments.items.filterNot { it.id in existingIds }
                    current.copy(
                        comments = current.comments + newUniqueItems,
                        commentsNextPageToken = pagedComments.nextPageToken,
                        isLoadingMoreComments = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMoreComments = false) }
            }
        }
    }

    fun onPlayerEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }
}
