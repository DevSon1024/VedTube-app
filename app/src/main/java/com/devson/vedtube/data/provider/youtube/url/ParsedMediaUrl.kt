package com.devson.vedtube.data.provider.youtube.url

sealed interface ParsedMediaUrl {
    data class Video(
        val videoId: String,
        val playlistId: String? = null,
        val timestampMs: Long? = null
    ) : ParsedMediaUrl

    data class Playlist(
        val playlistId: String
    ) : ParsedMediaUrl

    sealed interface Channel : ParsedMediaUrl {
        data class Id(val channelId: String) : Channel
        data class Handle(val handle: String) : Channel
        data class CustomUrl(val customUrl: String) : Channel
        data class User(val username: String) : Channel
    }

    data object Unknown : ParsedMediaUrl
}
