package com.devson.vedtube.data.provider.youtube.cobalt.model

import kotlinx.serialization.Serializable

@Serializable
data class CobaltResponse(
    val status: String? = null,
    val url: String? = null,
    val filename: String? = null,
    val picker: List<CobaltPickerItem> = emptyList(),
    val text: String? = null,
    val error: CobaltError? = null
)

@Serializable
data class CobaltPickerItem(
    val type: String? = null,
    val url: String? = null,
    val thumb: String? = null
)

@Serializable
data class CobaltError(
    val code: String? = null,
    val context: CobaltErrorContext? = null
)

@Serializable
data class CobaltErrorContext(
    val service: String? = null,
    val limit: Int? = null
)
