package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrServerGstProbeInfo(
    @SerialName("Container") val container: String? = null,
    @SerialName("DurationNS") val durationNS: Long? = null,
    @SerialName("FileSize") val fileSize: Long? = null,
    @SerialName("Tracks") val tracks: List<TorrServerGstTrack>? = null
)

@Serializable
data class TorrServerGstTrack(
    @SerialName("Index") val index: Int,
    @SerialName("Type") val type: String? = null, // "video", "audio", "subtitle"
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null
)
