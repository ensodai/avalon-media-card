package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.Serializable

@Serializable
data class TmdbVideoResponse(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = ""
)
