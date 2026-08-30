package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JackettResult(
    @SerialName(value = "Title")
    val title: String,
    @SerialName(value = "Link")
    val link: String? = null,
    @SerialName(value = "MagnetUri")
    val magnetUri: String? = null,
    @SerialName(value = "Size")
    val size: Long = 0,
    @SerialName(value = "Seeders")
    val seeders: Int = 0,
    @SerialName(value = "Peers")
    val peers: Int = 0,
    @SerialName(value = "Tracker")
    val tracker: String? = null
)

