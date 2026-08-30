package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TorrServerResponse(
    val hash: String? = null,
    val title: String? = null
)