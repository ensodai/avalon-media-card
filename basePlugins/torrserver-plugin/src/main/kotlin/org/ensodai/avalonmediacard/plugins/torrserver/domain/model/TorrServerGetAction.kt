package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TorrServerGetAction(
    val action: String,
    val hash: String
)