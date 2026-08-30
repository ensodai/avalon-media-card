package org.ensodai.avalonmediacard.core.player

import kotlinx.serialization.Serializable

@Serializable
data class AudioTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val channels: Int,
    val codec: String,
    val isDefault: Boolean = false
)

@Serializable
data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isExternal: Boolean,
    val mimeType: String
)
