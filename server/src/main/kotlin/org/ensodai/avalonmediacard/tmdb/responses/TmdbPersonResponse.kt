package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPersonResponse(
    val id: Int,
    val name: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val biography: String? = null
)
