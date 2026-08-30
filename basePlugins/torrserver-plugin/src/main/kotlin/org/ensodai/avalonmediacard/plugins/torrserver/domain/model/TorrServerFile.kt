package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TorrServerFile(
    val id: Int,
    val path: String,
    val length: Long
)