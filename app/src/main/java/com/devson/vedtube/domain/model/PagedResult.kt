package com.devson.vedtube.domain.model

/**
 * Generic container for paginated items.
 */
data class PagedResult<T>(
    val items: List<T> = emptyList(),
    val nextPageToken: String? = null
)
