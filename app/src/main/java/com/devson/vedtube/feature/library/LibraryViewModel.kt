package com.devson.vedtube.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.domain.repository.SubscriptionRepository
import com.devson.vedtube.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        watchHistoryRepository.getRecentHistory(),
        subscriptionRepository.getAllSubscriptions()
    ) { history, subscriptions ->
        LibraryUiState(
            historyList = history,
            subscriptionsList = subscriptions,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun deleteHistoryItem(videoId: String) {
        viewModelScope.launch(ioDispatcher) {
            watchHistoryRepository.deleteHistory(videoId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(ioDispatcher) {
            watchHistoryRepository.clearHistory()
        }
    }

    fun unsubscribe(channelId: String) {
        viewModelScope.launch(ioDispatcher) {
            subscriptionRepository.unsubscribe(channelId)
        }
    }
}
