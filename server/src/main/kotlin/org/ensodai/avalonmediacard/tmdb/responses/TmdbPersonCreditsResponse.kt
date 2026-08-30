package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPersonCreditsResponse(
    val cast: List<TmdbPersonCreditItem> = emptyList(),
    val crew: List<TmdbPersonCreditItem> = emptyList()
)

@Serializable
data class TmdbPersonCreditItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val job: String? = null,
    val department: String? = null,
    val character: String? = null
)
