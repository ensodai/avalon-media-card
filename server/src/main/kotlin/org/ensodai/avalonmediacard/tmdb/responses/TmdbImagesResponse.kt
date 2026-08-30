package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbImagesResponse(
    val backdrops: List<TmdbImageItemResponse> = emptyList(),
    val posters: List<TmdbImageItemResponse> = emptyList(),
    val logos: List<TmdbImageItemResponse> = emptyList()
)

@Serializable
data class TmdbImageItemResponse(
    @SerialName("file_path") val filePath: String,
    @SerialName("iso_639_1") val iso639: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("vote_count") val voteCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0
)
