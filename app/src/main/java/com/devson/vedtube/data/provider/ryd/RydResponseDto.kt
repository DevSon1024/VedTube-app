package com.devson.vedtube.data.provider.ryd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RydResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("likes") val likes: Long? = 0L,
    @SerialName("dislikes") val dislikes: Long? = 0L,
    @SerialName("rating") val rating: Double? = 0.0,
    @SerialName("viewCount") val viewCount: Long? = 0L,
    @SerialName("deleted") val deleted: Boolean? = false
)
