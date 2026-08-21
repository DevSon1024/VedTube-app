package com.devson.vedtube.domain.model

/**
 * Extended domain model representing detailed information about a video.
 */
data class VideoDetails(
    val id: String,
    val title: String,
    val description: String = "",
    val uploaderName: String,
    val uploaderId: String? = null,
    val uploaderAvatarUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0,
    val viewCount: Long = 0,
    val likeCount: Long? = null,
    val uploadDate: String? = null,
    val isLive: Boolean = false,
    val subscriberCount: Long? = null,
    val relatedVideos: List<Video> = emptyList()
) {
    fun toVideo(): Video = Video(
        id = id,
        title = title,
        uploaderName = uploaderName,
        uploaderId = uploaderId,
        uploaderAvatarUrl = uploaderAvatarUrl,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        viewCount = viewCount,
        uploadDate = uploadDate
    )
}
