package com.devson.vedtube.domain.model

/**
 * Represents an individual result item in a search query.
 */
sealed interface SearchItem {
    data class VideoItem(val video: Video) : SearchItem
    data class ChannelItem(val channel: Channel) : SearchItem
    data class PlaylistItem(val playlist: Playlist) : SearchItem
}

/**
 * Domain container for paginated search results.
 */
data class SearchResult(
    val query: String,
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null
)
