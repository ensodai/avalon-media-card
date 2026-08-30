package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbCreditsResponse(
    val cast: List<TmdbCastResponse> = emptyList(),
    val crew: List<TmdbCrewResponse> = emptyList()
)

@Serializable
data class TmdbCastResponse(
    val id: Int = 0,
    val name: String = "",
    @SerialName("original_name") val originalName: String? = null,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 0
)

@Serializable
data class TmdbCrewResponse(
    val id: Int = 0,
    val name: String = "",
    @SerialName("original_name") val originalName: String? = null,
    val job: String? = null,
    val department: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)
