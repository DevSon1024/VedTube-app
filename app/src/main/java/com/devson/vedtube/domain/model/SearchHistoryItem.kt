package com.devson.vedtube.domain.model

/**
 * Domain model representing a user's recent search query.
 */
data class SearchHistoryItem(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
