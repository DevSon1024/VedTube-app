package com.devson.vedtube.data.provider.youtube.cobalt.model

import kotlinx.serialization.Serializable

@Serializable
data class CobaltRequest(
    val url: String,
    val videoQuality: String = "720",
    val filenamePattern: String = "basic"
)
