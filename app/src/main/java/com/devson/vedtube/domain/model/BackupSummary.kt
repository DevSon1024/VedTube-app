package com.devson.vedtube.domain.model

/**
 * Summary metrics of an export or import operation.
 */
data class BackupSummary(
    val subscriptionsCount: Int = 0,
    val watchHistoryCount: Int = 0,
    val playlistsCount: Int = 0,
    val playlistVideosCount: Int = 0,
    val searchHistoryCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalItemsCount: Int
        get() = subscriptionsCount + watchHistoryCount + playlistsCount + playlistVideosCount + searchHistoryCount
}
