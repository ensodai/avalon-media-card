package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.Serializable

@Serializable
data class TmdbGenreResponse(
    val id: Int = 0,
    val name: String = ""
)
