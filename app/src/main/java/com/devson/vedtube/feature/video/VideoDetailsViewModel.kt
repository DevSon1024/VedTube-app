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
import com.devson.vedtube.domain.repository.PlaylistRepository
import com.devson.vedtube.domain.repository.RydRepository
import com.devson.vedtube.domain.repository.SettingsRepository
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
    private val playlistRepository: PlaylistRepository,
    private val rydRepository: RydRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackResolver: PlaybackResolver,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailsUiState())
    val uiState: StateFlow<VideoDetailsUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = vedPlayer.playerState
    private var subscriptionObserveJob: Job? = null
    private var downloadObserveJob: Job? = null
    private var playlistObserveJob: Job? = null
    private var rydJob: Job? = null

    init {
        // Observe watch history progress for related video thumbnails
        watchHistoryRepository.getRecentHistory().onEach { historyList ->
            val progressMap = historyList.associate { it.videoId to it.progressFraction }
            _uiState.update { it.copy(watchProgressMap = progressMap) }
        }.launchIn(viewModelScope)

        // Observe playlists
        playlistRepository.getAllPlaylists().onEach { playlists ->
            _uiState.update { it.copy(playlists = playlists) }
        }.launchIn(viewModelScope)

        // Observe Distraction-Free Mode from settings
        settingsRepository.distractionFreeMode.onEach { distractionFree ->
            _uiState.update { it.copy(isDistractionFreeMode = distractionFree) }
        }.launchIn(viewModelScope)

        // Observe Skip Interval from settings and propagate to player
        settingsRepository.skipIntervalSeconds.onEach { interval ->
            vedPlayer.setSkipInterval(interval)
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
                isCommentsSheetVisible = false,
                dislikesCount = null
            )
        }

        observeDownloadStatus(videoId)
        observePlaylistStatus(videoId)
        loadRydDislikes(videoId)

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

    private fun loadRydDislikes(videoId: String) {
        rydJob?.cancel()
        rydJob = viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                rydRepository.getDislikes(videoId)
            }
            result.onSuccess { dislikes ->
                _uiState.update { it.copy(dislikesCount = dislikes) }
            }.onFailure {
                // Graceful fallback: Keep dislikesCount null without affecting video experience
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
            // Already downloaded: delete/remove download
            viewModelScope.launch(ioDispatcher) {
                downloadRepository.deleteDownload(videoId)
            }
            return
        }

        if (currentDownload != null && currentDownload.isDownloading) {
            // Cancel downloading
            viewModelScope.launch(ioDispatcher) {
                downloadRepository.cancelDownload(videoId)
            }
            return
        }

        // Open download quality picker sheet
        _uiState.update {
            it.copy(
                isDownloadQualitySheetVisible = true,
                isLoadingDownloadStreams = true,
                availableDownloadStreams = emptyList()
            )
        }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                playbackResolver.resolve(videoId = videoId)
            }

            result.onSuccess { resolvedMedia ->
                _uiState.update {
                    it.copy(
                        isLoadingDownloadStreams = false,
                        availableDownloadStreams = resolvedMedia.streams
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingDownloadStreams = false,
                        availableDownloadStreams = emptyList()
                    )
                }
            }
        }
    }

    fun dismissDownloadQualitySheet() {
        _uiState.update { it.copy(isDownloadQualitySheetVisible = false) }
    }

    fun onSelectDownloadQuality(stream: VideoStream) = startDownload(stream)

    fun startDownload(stream: VideoStream) {
        val details = _uiState.value.details ?: return
        val video = Video(
            id = details.id,
            title = details.title,
            uploaderName = details.uploaderName,
            thumbnailUrl = details.thumbnailUrl ?: "",
            durationSeconds = details.durationSeconds
        )

        _uiState.update { it.copy(isDownloadQualitySheetVisible = false) }

        viewModelScope.launch(ioDispatcher) {
            downloadRepository.startDownload(
                video = video,
                stream = stream
            )
        }
    }

    fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun onRelatedVideoClick(video: Video) {
        vedPlayer.playVideo(video)
        loadVideo(video.id)
    }

    fun openComments() {
        _uiState.update { it.copy(isCommentsSheetVisible = true) }
        if (_uiState.value.comments.isEmpty()) {
            loadComments(_uiState.value.videoId)
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

    private fun observePlaylistStatus(videoId: String) {
        playlistObserveJob?.cancel()
        playlistObserveJob = playlistRepository.getPlaylistIdsContainingVideo(videoId)
            .onEach { containingIds ->
                _uiState.update { it.copy(containingPlaylistIds = containingIds) }
            }
            .launchIn(viewModelScope)
    }

    fun openSaveToPlaylist() {
        _uiState.update { it.copy(isSaveToPlaylistSheetVisible = true) }
    }

    fun dismissSaveToPlaylist() {
        _uiState.update { it.copy(isSaveToPlaylistSheetVisible = false) }
    }

    fun createPlaylistAndAddVideo(name: String) {
        val details = _uiState.value.details ?: return
        val currentVideo = Video(
            id = details.id,
            title = details.title,
            uploaderName = details.uploaderName,
            thumbnailUrl = details.thumbnailUrl ?: "",
            durationSeconds = details.durationSeconds
        )
        viewModelScope.launch {
            val newPlaylistId = playlistRepository.createPlaylist(name)
            playlistRepository.addVideoToPlaylist(newPlaylistId, currentVideo)
        }
    }

    fun toggleVideoInPlaylist(playlistId: String, isCurrentlyContained: Boolean) {
        val details = _uiState.value.details ?: return
        val currentVideo = Video(
            id = details.id,
            title = details.title,
            uploaderName = details.uploaderName,
            thumbnailUrl = details.thumbnailUrl ?: "",
            durationSeconds = details.durationSeconds
        )
        viewModelScope.launch {
            if (isCurrentlyContained) {
                playlistRepository.removeVideoFromPlaylist(playlistId, currentVideo.id)
            } else {
                playlistRepository.addVideoToPlaylist(playlistId, currentVideo)
            }
        }
    }

    fun onPlayerEvent(event: PlayerEvent) {
        vedPlayer.handleEvent(event)
    }
}
