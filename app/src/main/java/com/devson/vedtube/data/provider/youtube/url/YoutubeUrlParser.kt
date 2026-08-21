package com.devson.vedtube.data.provider.youtube.url

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object YoutubeUrlParser {

    private val VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$")
    private val PLAYLIST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,}$")
    private val CHANNEL_ID_PATTERN = Pattern.compile("^UC[a-zA-Z0-9_-]{22}$")
    private val HANDLE_PATTERN = Pattern.compile("^@[a-zA-Z0-9._-]{3,}$")
    private val URL_IN_TEXT_PATTERN = Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE)

    private val TIME_COMPOUND_PATTERN = Pattern.compile(
        "^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val SUPPORTED_YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "gaming.youtube.com",
        "youtu.be",
        "www.youtu.be"
    )

    /**
     * Parses a raw URL string or shared text containing a YouTube URL into a [ParsedMediaUrl].
     */
    fun parse(input: String?): ParsedMediaUrl {
        if (input.isNullOrBlank()) return ParsedMediaUrl.Unknown

        val extractedUrl = extractUrl(input.trim()) ?: return ParsedMediaUrl.Unknown

        return try {
            val normalizedUrl = if (!extractedUrl.startsWith("http://", ignoreCase = true) &&
                !extractedUrl.startsWith("https://", ignoreCase = true)
            ) {
                "https://$extractedUrl"
            } else {
                extractedUrl
            }

            val uri = URI(normalizedUrl)
            val host = uri.host?.lowercase() ?: return ParsedMediaUrl.Unknown

            if (!isValidYouTubeHost(host)) {
                return ParsedMediaUrl.Unknown
            }

            parseUri(uri, host)
        } catch (e: Exception) {
            ParsedMediaUrl.Unknown
        }
    }

    private fun extractUrl(text: String): String? {
        val matcher = URL_IN_TEXT_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(0)
        } else if (text.contains("youtube.com") || text.contains("youtu.be")) {
            text.split("\\s+".toRegex()).firstOrNull { it.contains("youtube.com") || it.contains("youtu.be") }
        } else {
            null
        }
    }

    private fun isValidYouTubeHost(host: String): Boolean {
        if (SUPPORTED_YOUTUBE_HOSTS.contains(host)) return true
        return SUPPORTED_YOUTUBE_HOSTS.any { validHost ->
            host.endsWith(".$validHost")
        }
    }

    private fun parseUri(uri: URI, host: String): ParsedMediaUrl {
        val path = uri.path ?: ""
        val pathSegments = path.split("/").filter { it.isNotEmpty() }.map { decode(it) }
        val queryParams = extractQueryParams(uri.rawQuery)
        val fragment = uri.fragment

        // Short link: youtu.be/<videoId>
        if (host == "youtu.be" || host == "www.youtu.be") {
            val videoId = pathSegments.firstOrNull()
            return if (isValidVideoId(videoId)) {
                val timestampMs = parseTimestamp(queryParams["t"] ?: queryParams["start"] ?: extractFragmentTimestamp(fragment))
                val playlistId = queryParams["list"]?.takeIf { isValidPlaylistId(it) }
                ParsedMediaUrl.Video(
                    videoId = videoId!!,
                    playlistId = playlistId,
                    timestampMs = timestampMs
                )
            } else {
                ParsedMediaUrl.Unknown
            }
        }

        // Standard YouTube domain paths
        val firstSegment = pathSegments.firstOrNull()?.lowercase() ?: return parseQueryOnly(queryParams)

        return when (firstSegment) {
            "watch" -> {
                val videoId = queryParams["v"]
                val playlistId = queryParams["list"]?.takeIf { isValidPlaylistId(it) }
                val timestampMs = parseTimestamp(
                    queryParams["t"]
                        ?: queryParams["start"]
                        ?: extractFragmentTimestamp(fragment)
                )

                if (isValidVideoId(videoId)) {
                    ParsedMediaUrl.Video(
                        videoId = videoId!!,
                        playlistId = playlistId,
                        timestampMs = timestampMs
                    )
                } else if (playlistId != null) {
                    ParsedMediaUrl.Playlist(playlistId = playlistId)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "shorts" -> {
                val videoId = pathSegments.getOrNull(1)
                if (isValidVideoId(videoId)) {
                    val timestampMs = parseTimestamp(queryParams["t"] ?: queryParams["start"])
                    ParsedMediaUrl.Video(videoId = videoId!!, timestampMs = timestampMs)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "live" -> {
                val videoId = pathSegments.getOrNull(1)
                if (isValidVideoId(videoId)) {
                    val timestampMs = parseTimestamp(queryParams["t"] ?: queryParams["start"])
                    ParsedMediaUrl.Video(videoId = videoId!!, timestampMs = timestampMs)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "embed", "v" -> {
                val videoId = pathSegments.getOrNull(1)
                if (isValidVideoId(videoId)) {
                    val timestampMs = parseTimestamp(queryParams["t"] ?: queryParams["start"])
                    val playlistId = queryParams["list"]?.takeIf { isValidPlaylistId(it) }
                    ParsedMediaUrl.Video(
                        videoId = videoId!!,
                        playlistId = playlistId,
                        timestampMs = timestampMs
                    )
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "playlist" -> {
                val playlistId = queryParams["list"]
                if (isValidPlaylistId(playlistId)) {
                    ParsedMediaUrl.Playlist(playlistId = playlistId!!)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "channel" -> {
                val channelId = pathSegments.getOrNull(1)
                if (!channelId.isNullOrBlank() && (isValidChannelId(channelId) || channelId.matches("^[a-zA-Z0-9_-]+$".toRegex()))) {
                    ParsedMediaUrl.Channel.Id(channelId = channelId)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "c" -> {
                val customUrl = pathSegments.getOrNull(1)
                if (!customUrl.isNullOrBlank()) {
                    ParsedMediaUrl.Channel.CustomUrl(customUrl = customUrl)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            "user" -> {
                val username = pathSegments.getOrNull(1)
                if (!username.isNullOrBlank()) {
                    ParsedMediaUrl.Channel.User(username = username)
                } else {
                    ParsedMediaUrl.Unknown
                }
            }

            else -> {
                // Check if segment is a handle: @username
                if (firstSegment.startsWith("@")) {
                    val handle = pathSegments.first()
                    if (isValidHandle(handle)) {
                        ParsedMediaUrl.Channel.Handle(handle = handle)
                    } else {
                        ParsedMediaUrl.Unknown
                    }
                } else {
                    parseQueryOnly(queryParams)
                }
            }
        }
    }

    private fun parseQueryOnly(queryParams: Map<String, String>): ParsedMediaUrl {
        val videoId = queryParams["v"]
        val playlistId = queryParams["list"]?.takeIf { isValidPlaylistId(it) }
        val timestampMs = parseTimestamp(queryParams["t"] ?: queryParams["start"])

        return if (isValidVideoId(videoId)) {
            ParsedMediaUrl.Video(
                videoId = videoId!!,
                playlistId = playlistId,
                timestampMs = timestampMs
            )
        } else if (playlistId != null) {
            ParsedMediaUrl.Playlist(playlistId = playlistId)
        } else {
            ParsedMediaUrl.Unknown
        }
    }

    private fun extractQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val params = mutableMapOf<String, String>()
        val pairs = rawQuery.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = decode(pair.substring(0, idx))
                val value = decode(pair.substring(idx + 1))
                params[key] = value
            } else if (pair.isNotEmpty()) {
                params[decode(pair)] = ""
            }
        }
        return params
    }

    /**
     * Parses timestamp string into milliseconds.
     * Supports: "90", "90s", "1h2m3s", "2m30s", "45s", "1h"
     */
    fun parseTimestamp(timeString: String?): Long? {
        if (timeString.isNullOrBlank()) return null
        val clean = timeString.trim().removeSuffix("s").trim()

        // Direct integer seconds
        clean.toLongOrNull()?.let { seconds ->
            return if (seconds >= 0) seconds * 1000L else null
        }

        // Compound format: e.g. 1h2m3s, 1h30s, 2m10s
        val matcher = TIME_COMPOUND_PATTERN.matcher(timeString.trim())
        if (matcher.matches()) {
            val hours = matcher.group(1)?.toLongOrNull() ?: 0L
            val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
            val seconds = matcher.group(3)?.toLongOrNull() ?: 0L

            val totalSeconds = (hours * 3600) + (minutes * 60) + seconds
            if (totalSeconds > 0 || timeString.trim() == "0s" || timeString.trim() == "0") {
                return totalSeconds * 1000L
            }
        }

        return null
    }

    private fun extractFragmentTimestamp(fragment: String?): String? {
        if (fragment.isNullOrBlank()) return null
        val parts = fragment.split("&")
        for (part in parts) {
            if (part.startsWith("t=")) {
                return part.removePrefix("t=")
            } else if (part.startsWith("start=")) {
                return part.removePrefix("start=")
            }
        }
        return null
    }

    private fun decode(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }
    }

    fun isValidVideoId(videoId: String?): Boolean {
        if (videoId.isNullOrBlank()) return false
        return VIDEO_ID_PATTERN.matcher(videoId).matches()
    }

    fun isValidPlaylistId(playlistId: String?): Boolean {
        if (playlistId.isNullOrBlank()) return false
        return PLAYLIST_ID_PATTERN.matcher(playlistId).matches()
    }

    fun isValidChannelId(channelId: String?): Boolean {
        if (channelId.isNullOrBlank()) return false
        return CHANNEL_ID_PATTERN.matcher(channelId).matches()
    }

    fun isValidHandle(handle: String?): Boolean {
        if (handle.isNullOrBlank()) return false
        return HANDLE_PATTERN.matcher(handle).matches()
    }
}
