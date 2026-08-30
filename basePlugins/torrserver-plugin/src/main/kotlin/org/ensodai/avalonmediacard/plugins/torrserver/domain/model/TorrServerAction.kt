package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrServerAction(
    val action: String,
    val link: String? = null,
    val hash: String? = null,
    @SerialName("save_to_db")
    val saveToDb: Boolean = true
)