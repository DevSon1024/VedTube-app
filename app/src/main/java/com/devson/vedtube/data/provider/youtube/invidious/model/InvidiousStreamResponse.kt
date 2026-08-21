package com.devson.vedtube.data.provider.youtube.invidious.model

import kotlinx.serialization.Serializable

@Serializable
data class InvidiousStreamResponse(
    val title: String? = null,
    val description: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val lengthSeconds: Long? = null,
    val hlsUrl: String? = null,
    val dashUrl: String? = null,
    val formatStreams: List<InvidiousFormatStream> = emptyList(),
    val adaptiveFormats: List<InvidiousAdaptiveFormat> = emptyList(),
    val captions: List<InvidiousCaption> = emptyList()
)

@Serializable
data class InvidiousFormatStream(
    val url: String? = null,
    val quality: String? = null,
    val type: String? = null,
    val container: String? = null,
    val resolution: String? = null,
    val qualityLabel: String? = null,
    val bitrate: String? = null,
    val fps: Int? = null
)

@Serializable
data class InvidiousAdaptiveFormat(
    val url: String? = null,
    val type: String? = null,
    val qualityLabel: String? = null,
    val resolution: String? = null,
    val bitrate: String? = null,
    val container: String? = null,
    val fps: Int? = null
)

@Serializable
data class InvidiousCaption(
    val url: String? = null,
    val label: String? = null,
    val languageCode: String? = null
)
