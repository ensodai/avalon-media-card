package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProwlarrResult(
    val title: String,
    val downloadUrl: String? = null,
    val infoUrl: String? = null,
    val size: Long = 0,
    val seeders: Int = 0,
    val leechers: Int = 0,
    val indexer: String? = null,
    val guid: String? = null
)