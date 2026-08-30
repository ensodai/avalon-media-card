package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrServerFilesResponse(
    @SerialName("file_stats")
    val fileStats: List<TorrServerFile>? = null
)