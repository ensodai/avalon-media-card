package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.Serializable

@Serializable
data class TmdbResultResponse<T>(
    val results: List<T> = emptyList()
)

@Serializable
data class TmdbPageResultResponse<T>(
    val page: Int,
    val results: List<T> = emptyList(),
    val total_pages: Int = 0,
    val total_results: Int = 0
)
