package org.ensodai.avalonmediacard.plugins.mediadetails.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.plugins.mediadetails.domain.*

class MediaDetailsStateManager(
    private val scope: CoroutineScope,
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val getSimilarUseCase: GetSimilarUseCase,
    private val getSeasonDetailsUseCase: GetSeasonDetailsUseCase
) {
    private val mediaDetailsStatesMap =
        java.util.concurrent.ConcurrentHashMap<MediaKey, MutableStateFlow<MediaDetailsState>>()

    fun getMediaDetailsState(key: MediaKey): StateFlow<MediaDetailsState> {
        return mediaDetailsStatesMap.getOrPut(key) { MutableStateFlow(MediaDetailsState(isLoading = true)) }
            .asStateFlow()
    }

    private val recommendationsStatesMap =
        java.util.concurrent.ConcurrentHashMap<MediaKey, MutableStateFlow<DetailsWidgetState>>()

    fun getRecommendationsState(key: MediaKey): StateFlow<DetailsWidgetState> {
        return recommendationsStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState(isLoading = true)) }
            .asStateFlow()
    }

    private val similarStatesMap =
        java.util.concurrent.ConcurrentHashMap<MediaKey, MutableStateFlow<DetailsWidgetState>>()

    fun getSimilarState(key: MediaKey): StateFlow<DetailsWidgetState> {
        return similarStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState(isLoading = true)) }.asStateFlow()
    }

    fun loadMediaDetailsInitial(key: MediaKey, force: Boolean = false, language: String = "ru") {
        val flow = mediaDetailsStatesMap.getOrPut(key) { MutableStateFlow(MediaDetailsState(isLoading = true)) }
        if (flow.value.metadata != null && !force) {
            val metadata = flow.value.metadata
            if (metadata != null && metadata.seasons.isNotEmpty()) {
                val currentSelected = flow.value.selectedSeasonNumber
                val targetSeason = metadata.seasons.firstOrNull { it.seasonNumber == currentSelected }
                    ?: metadata.seasons.firstOrNull { it.seasonNumber == 1 }
                    ?: metadata.seasons.first()
                if (flow.value.seasonContents[targetSeason.seasonNumber]?.episodes.isNullOrEmpty()) {
                    loadSeasonDetails(key, targetSeason.seasonNumber, isInitial = true, language = language)
                }
            }
            return
        }

        flow.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val metadata = getMediaDetailsUseCase(key, language)
                flow.update { currentState ->
                    currentState.copy(
                        metadata = metadata,
                        isLoading = false,
                        error = null
                    )
                }

                // Если это сериал и есть сезоны, сразу грузим первый сезон (или 1-й по счету)
                if (metadata.seasons.isNotEmpty()) {
                    val currentSelected = flow.value.selectedSeasonNumber
                    val firstSeason = metadata.seasons.firstOrNull { it.seasonNumber == currentSelected }
                        ?: metadata.seasons.firstOrNull { it.seasonNumber == 1 }
                        ?: metadata.seasons.first()
                    loadSeasonDetails(key, firstSeason.seasonNumber, isInitial = true, language = language)
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message ?: "Ошибка получения деталей фильма") }
            }
        }
    }

    fun loadSeasonDetails(key: MediaKey, seasonNumber: Int, isInitial: Boolean = false, language: String = "ru") {
        var shouldLoad = false
        val flow = mediaDetailsStatesMap.getOrPut(key) { MutableStateFlow(MediaDetailsState()) }
        
        flow.update { currentState ->
            if (currentState.seasonContents[seasonNumber]?.isLoading == true) return@update currentState

            val existingEpisodes = currentState.seasonContents[seasonNumber]?.episodes
            val hasExistingEpisodes = !existingEpisodes.isNullOrEmpty()
            shouldLoad = !hasExistingEpisodes

            val currentContents = currentState.seasonContents.toMutableMap()
            currentContents[seasonNumber] = org.ensodai.avalonmediacard.contract.slot.SeasonContent(
                isLoading = !hasExistingEpisodes,
                episodes = existingEpisodes
            )
            currentState.copy(
                selectedSeasonNumber = seasonNumber,
                seasonContents = currentContents
            )
        }
        if (!shouldLoad) return

        scope.launch {
            try {
                val episodes = getSeasonDetailsUseCase(key, seasonNumber, language)
                flow.update { currentState ->
                    val updatedContents = currentState.seasonContents.toMutableMap()
                    updatedContents[seasonNumber] = org.ensodai.avalonmediacard.contract.slot.SeasonContent(
                        isLoading = false,
                        episodes = episodes
                    )
                    currentState.copy(seasonContents = updatedContents)
                }
            } catch (e: Exception) {
                flow.update { currentState ->
                    val updatedContents = currentState.seasonContents.toMutableMap()
                    updatedContents[seasonNumber] = org.ensodai.avalonmediacard.contract.slot.SeasonContent(
                        isLoading = false,
                        episodes = updatedContents[seasonNumber]?.episodes
                    )
                    currentState.copy(seasonContents = updatedContents)
                }
            }
        }
    }

    fun loadRecommendationsInitial(key: MediaKey, language: String = "ru") {
        val flow = recommendationsStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState(isLoading = true)) }
        if (flow.value.movies.isNotEmpty()) return

        flow.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val movies = getRecommendationsUseCase(key, 1, language)
                flow.update { it.copy(movies = movies, page = 1, isLoading = false, error = null) }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message ?: "Ошибка получения рекомендаций") }
            }
        }
    }

    fun loadMoreRecommendations(key: MediaKey, page: Int, language: String = "ru") {
        var shouldLoad = false
        val flow = recommendationsStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState()) }
        flow.update { currentState ->
            if (currentState.isLoading || currentState.page == page) {
                currentState
            } else {
                shouldLoad = true
                currentState.copy(isLoading = true)
            }
        }
        if (!shouldLoad) return

        scope.launch {
            try {
                val newMovies = getRecommendationsUseCase(key, page, language)
                flow.update { currentState ->
                    if (newMovies.isNotEmpty()) {
                        val combined = (currentState.movies + newMovies).distinctBy { it.id }
                        currentState.copy(
                            movies = combined,
                            page = page,
                            isLoading = false
                        )
                    } else {
                        currentState.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message ?: "Ошибка получения данных") }
            }
        }
    }

    fun loadSimilarInitial(key: MediaKey, language: String = "ru") {
        val flow = similarStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState(isLoading = true)) }
        if (flow.value.movies.isNotEmpty()) return

        flow.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val movies = getSimilarUseCase(key, 1, language)
                flow.update { it.copy(movies = movies, page = 1, isLoading = false, error = null) }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message ?: "Ошибка получения похожих фильмов") }
            }
        }
    }

    fun loadMoreSimilar(key: MediaKey, page: Int, language: String = "ru") {
        var shouldLoad = false
        val flow = similarStatesMap.getOrPut(key) { MutableStateFlow(DetailsWidgetState()) }
        flow.update { currentState ->
            if (currentState.isLoading || currentState.page == page) {
                currentState
            } else {
                shouldLoad = true
                currentState.copy(isLoading = true)
            }
        }
        if (!shouldLoad) return

        scope.launch {
            try {
                val newMovies = getSimilarUseCase(key, page, language)
                flow.update { currentState ->
                    if (newMovies.isNotEmpty()) {
                        val combined = (currentState.movies + newMovies).distinctBy { it.id }
                        currentState.copy(
                            movies = combined,
                            page = page,
                            isLoading = false
                        )
                    } else {
                        currentState.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message ?: "Ошибка получения данных") }
            }
        }
    }
}
