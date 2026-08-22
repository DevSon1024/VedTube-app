package com.devson.vedtube.data.provider.sponsorblock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SponsorBlockSegmentResponse(
    val category: String,
    val actionType: String? = null,
    val segment: List<Double>,
    @SerialName("UUID") val uuid: String? = null,
    val videoDuration: Double? = null
)
