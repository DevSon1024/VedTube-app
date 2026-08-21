package com.devson.vedtube.core.player

import com.devson.vedtube.domain.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback queue, supporting dynamic insertion, deletion, reordering,
 * deterministic and non-destructive shuffle, and repeat modes (OFF, ALL, ONE).
 */
@Singleton
class QueueManager @Inject constructor() {

    private val _queue = MutableStateFlow<List<Video>>(emptyList())
    val queue: StateFlow<List<Video>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo: StateFlow<Video?> = _currentVideo.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Stores the original unshuffled list when shuffle is enabled
    private var originalQueue: List<Video> = emptyList()

    @Synchronized
    fun setQueue(videos: List<Video>, startIndex: Int = 0): Video? {
        originalQueue = videos.toList()
        val targetIndex = if (videos.isNotEmpty()) startIndex.coerceIn(0, videos.lastIndex) else -1

        if (_isShuffleEnabled.value && videos.size > 1) {
            val currentItem = if (targetIndex in videos.indices) videos[targetIndex] else null
            val remaining = videos.toMutableList().apply {
                if (currentItem != null) remove(currentItem)
            }.shuffled()
            val newShuffled = if (currentItem != null) listOf(currentItem) + remaining else remaining
            _queue.value = newShuffled
            _currentIndex.value = if (newShuffled.isNotEmpty()) 0 else -1
        } else {
            _queue.value = originalQueue
            _currentIndex.value = targetIndex
        }

        updateCurrentVideo()
        return _currentVideo.value
    }

    @Synchronized
    fun add(video: Video) {
        val updated = _queue.value.toMutableList().apply { add(video) }
        _queue.value = updated
        originalQueue = originalQueue.toMutableList().apply { add(video) }

        if (_currentIndex.value == -1 && updated.isNotEmpty()) {
            _currentIndex.value = 0
            updateCurrentVideo()
        }
    }

    @Synchronized
    fun addAll(videos: List<Video>) {
        if (videos.isEmpty()) return
        val updated = _queue.value.toMutableList().apply { addAll(videos) }
        _queue.value = updated
        originalQueue = originalQueue.toMutableList().apply { addAll(videos) }

        if (_currentIndex.value == -1 && updated.isNotEmpty()) {
            _currentIndex.value = 0
            updateCurrentVideo()
        }
    }

    @Synchronized
    fun addNext(video: Video) {
        val currentIdx = _currentIndex.value
        val insertIdx = if (currentIdx in _queue.value.indices) currentIdx + 1 else _queue.value.size
        val updated = _queue.value.toMutableList().apply { add(insertIdx, video) }
        _queue.value = updated
        originalQueue = originalQueue.toMutableList().apply { add(video) }

        if (_currentIndex.value == -1 && updated.isNotEmpty()) {
            _currentIndex.value = 0
            updateCurrentVideo()
        }
    }

    @Synchronized
    fun remove(index: Int): Boolean {
        val list = _queue.value
        if (index !in list.indices) return false

        val itemToRemove = list[index]
        val updated = list.toMutableList().apply { removeAt(index) }
        _queue.value = updated
        originalQueue = originalQueue.toMutableList().apply { remove(itemToRemove) }

        val currentIdx = _currentIndex.value
        when {
            updated.isEmpty() -> {
                _currentIndex.value = -1
            }
            index < currentIdx -> {
                _currentIndex.value = currentIdx - 1
            }
            index == currentIdx -> {
                _currentIndex.value = currentIdx.coerceAtMost(updated.lastIndex)
            }
        }

        updateCurrentVideo()
        return true
    }

    @Synchronized
    fun move(fromIndex: Int, toIndex: Int): Boolean {
        val list = _queue.value
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return false

        val mutable = list.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        _queue.value = mutable

        val currentIdx = _currentIndex.value
        when (currentIdx) {
            fromIndex -> _currentIndex.value = toIndex
            in (fromIndex + 1)..toIndex -> _currentIndex.value = currentIdx - 1
            in toIndex until fromIndex -> _currentIndex.value = currentIdx + 1
        }

        updateCurrentVideo()
        return true
    }

    @Synchronized
    fun clear() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentIndex.value = -1
        _currentVideo.value = null
    }

    @Synchronized
    fun next(): Video? {
        val list = _queue.value
        if (list.isEmpty()) return null

        val current = _currentIndex.value
        val nextIndex = when (_repeatMode.value) {
            RepeatMode.ONE -> current
            RepeatMode.ALL -> if (current >= list.lastIndex) 0 else current + 1
            RepeatMode.OFF -> if (current < list.lastIndex) current + 1 else -1
        }

        if (nextIndex in list.indices) {
            _currentIndex.value = nextIndex
            updateCurrentVideo()
            return _currentVideo.value
        }
        return null
    }

    @Synchronized
    fun previous(): Video? {
        val list = _queue.value
        if (list.isEmpty()) return null

        val current = _currentIndex.value
        val prevIndex = when (_repeatMode.value) {
            RepeatMode.ONE -> current
            RepeatMode.ALL -> if (current <= 0) list.lastIndex else current - 1
            RepeatMode.OFF -> if (current > 0) current - 1 else -1
        }

        if (prevIndex in list.indices) {
            _currentIndex.value = prevIndex
            updateCurrentVideo()
            return _currentVideo.value
        }
        return null
    }

    @Synchronized
    fun setIndex(index: Int): Video? {
        val list = _queue.value
        if (index in list.indices) {
            _currentIndex.value = index
            updateCurrentVideo()
            return _currentVideo.value
        }
        return null
    }

    @Synchronized
    fun setShuffle(enabled: Boolean) {
        if (_isShuffleEnabled.value == enabled) return
        _isShuffleEnabled.value = enabled

        val current = _currentVideo.value
        if (enabled) {
            val list = _queue.value
            if (list.size > 1) {
                originalQueue = list.toList()
                val remaining = list.toMutableList().apply {
                    if (current != null) remove(current)
                }.shuffled()
                val newShuffled = if (current != null) listOf(current) + remaining else remaining
                _queue.value = newShuffled
                _currentIndex.value = if (newShuffled.isNotEmpty()) 0 else -1
            }
        } else {
            _queue.value = originalQueue
            _currentIndex.value = if (current != null) originalQueue.indexOf(current) else -1
        }
        updateCurrentVideo()
    }

    @Synchronized
    fun toggleShuffle(): Boolean {
        setShuffle(!_isShuffleEnabled.value)
        return _isShuffleEnabled.value
    }

    @Synchronized
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    @Synchronized
    fun toggleRepeatMode(): RepeatMode {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        return next
    }

    fun hasNext(): Boolean {
        val list = _queue.value
        if (list.isEmpty()) return false
        return when (_repeatMode.value) {
            RepeatMode.ALL -> true
            RepeatMode.ONE -> _currentIndex.value in list.indices
            RepeatMode.OFF -> _currentIndex.value < list.lastIndex
        }
    }

    fun hasPrevious(): Boolean {
        val list = _queue.value
        if (list.isEmpty()) return false
        return when (_repeatMode.value) {
            RepeatMode.ALL -> true
            RepeatMode.ONE -> _currentIndex.value in list.indices
            RepeatMode.OFF -> _currentIndex.value > 0
        }
    }

    private fun updateCurrentVideo() {
        val idx = _currentIndex.value
        val list = _queue.value
        _currentVideo.value = if (idx in list.indices) list[idx] else null
    }
}
