package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPersonDetailResponse(
    val id: Int,
    val name: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("combined_credits") val combinedCredits: TmdbPersonCreditsResponse? = null
)
