package org.ensodai.avalonmediacard.plugins.mediadetails.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.ensodai.avalonmediacard.contract.i18n.PluginI18n
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.resolveTargetLanguage
import org.ensodai.avalonmediacard.contract.model.withLocalizedGenres
import org.ensodai.avalonmediacard.contract.model.withUserSettings
import org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider
import org.ensodai.avalonmediacard.contract.plugins.UserGlobalSettingsProvider
import org.ensodai.avalonmediacard.contract.plugins.UserMovieProvider
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.plugins.mediadetails.LoadMoreRecommendations
import org.ensodai.avalonmediacard.plugins.mediadetails.LoadMoreSimilar
import org.ensodai.avalonmediacard.plugins.mediadetails.useractions.RetryLoadMediaDetailsCommand
import kotlin.uuid.Uuid

class MediaDetailsSlotFactory(
    private val pluginId: String,
    private val stateManager: MediaDetailsStateManager,
    private val i18n: PluginI18n,
    private val genreDictionaryProvider: GenreDictionaryProvider? = null
) {
    private val logger = AppLogging.logger("MediaDetailsSlotFactory")
    fun buildHeaderFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getMediaDetailsState(key)
            .map { state ->
                val meta = state.metadata
                val data = if (state.isLoading || meta == null) {
                    SlotData.Header(title = "")
                } else {
                    val userSettings = if (userId != null && userSettingsProvider != null) {
                        runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                    } else null
                    val customized = meta.withUserSettings(userSettings)
                    val userLocale = userSettings?.uiLocale ?: "ru"
                    val localizedGenres = genreDictionaryProvider?.getLocalizedGenres(userLocale)
                    val mappedGenres = customized.genres.withLocalizedGenres(localizedGenres)
                    SlotData.Header(
                        title = customized.title,
                        originalTitle = customized.originalTitle,
                        subtitle = customized.subtitle,
                        tagline = customized.tagline,
                        rating = customized.rating?.toDoubleOrNull(),
                        ratings = customized.rating?.let { listOf(MediaRatingItem("TMDB", it)) } ?: emptyList(),
                        genres = mappedGenres.map {
                            GenreItem(
                                id = it.id.toString(),
                                name = it.name,
                                clickAction = null
                            )
                        },
                        releaseDate = customized.releaseDate,
                        posterUrl = customized.posterUrl,
                        backgroundUrl = customized.backgroundUrl,
                        trailerUrl = customized.trailers.firstOrNull()?.videoUrl,
                        mediaType = key.type.name,
                        status = customized.status
                    )
                }
                val slotState = when {
                    state.error != null -> SlotState.Error(
                        message = state.error,
                        retryAction = RetryLoadMediaDetailsCommand(key)
                    )

                    state.isLoading || meta == null -> SlotState.Loading()
                    else -> SlotState.Content(data)
                }
                val stateType = if (state.isLoading) "Loading" else "Content"
                logger.d { "[PROFILING] SERVER buildHeaderFlow emit $stateType: ${kotlin.time.Clock.System.now()}" }
                SlotUpdate(slotId = SlotId.Header, nodeId = pluginId, state = slotState)
            }
            .distinctUntilChanged()
            .onStart {
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val targetLang = userSettings.resolveTargetLanguage()
                logger.d { "[PROFILING] SERVER buildHeaderFlow onStart emit Loading: ${kotlin.time.Clock.System.now()}" }
                emit(SlotUpdate(slotId = SlotId.Header, nodeId = pluginId, state = SlotState.Loading()))
                stateManager.loadMediaDetailsInitial(key, language = targetLang)
            }
    }

    fun buildDescriptionFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getMediaDetailsState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val customized = state.metadata?.withUserSettings(userSettings)
                val text = customized?.description ?: ""
                val slotState = when {
                    state.error != null -> SlotState.Empty
                    state.isLoading -> SlotState.Loading()
                    text.isNotBlank() -> SlotState.Content(SlotData.Text(text))
                    else -> SlotState.Empty
                }
                SlotUpdate(slotId = SlotId.Description, nodeId = pluginId, state = slotState)
            }
            .distinctUntilChanged()
            .onStart {
                emit(SlotUpdate(slotId = SlotId.Description, nodeId = pluginId, state = SlotState.Loading()))
            }
    }

    fun buildCastFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getMediaDetailsState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val customized = state.metadata?.withUserSettings(userSettings)
                val cast = customized?.cast ?: state.metadata?.cast ?: emptyList()
                val slotState = when {
                    state.error != null -> SlotState.Empty
                    state.isLoading && cast.isEmpty() -> SlotState.Loading()
                    cast.isEmpty() -> SlotState.Empty
                    else -> {
                        val members = cast.take(15).map { c ->
                            CastMemberItem(
                                key = MediaKey(MediaProvider.Tmdb, EntityType.PERSON, c.id ?: ""),
                                name = c.name,
                                character = c.character,
                                profileUrl = c.profileUrl
                            )
                        }
                        SlotState.Content(SlotData.Cast(i18n.t("details.cast_title"), members))
                    }
                }
                SlotUpdate(slotId = SlotId.Cast, nodeId = pluginId, state = slotState)
            }
            .distinctUntilChanged()
            .onStart {
                val dummyMembers = (1..6).map { CastMemberItem(key, "") }
                emit(SlotUpdate(slotId = SlotId.Cast, nodeId = pluginId, state = SlotState.Loading()))
            }
    }

    fun buildTvSeasonsFlow(key: MediaKey, userId: Uuid?, userMovies: UserMovieProvider?): Flow<SlotUpdate> {
        val tmdbFlow = stateManager.getMediaDetailsState(key)

        val userTriggerFlow = if (userId != null && userMovies != null) {
            userMovies.observeUserMovies(userId).map { } // emit Unit on changes
        } else {
            kotlinx.coroutines.flow.flowOf(Unit)
        }

        return kotlinx.coroutines.flow.combine(tmdbFlow, userTriggerFlow) { state, _ ->
            val userEpisodes = if (userId != null && userMovies != null) {
                try {
                    userMovies.getUserEpisodes(userId, key.id)
                } catch (e: Exception) {
                    logger.e(e) { "[buildTvSeasonsFlow] Error getting userEpisodes: ${e.message}" }
                    emptyList()
                }
            } else {
                emptyList()
            }

            val mappedSeasonContents = state.seasonContents.mapValues { (seasonNumber, content) ->
                val mappedEpisodes = content.episodes?.map { ep ->
                    val userEp = userEpisodes.find { it.season == seasonNumber && it.episode == ep.episodeNumber }
                    val isWatched = userEp?.isWatched ?: false
                    ep.copy(
                        isWatched = isWatched,
                        userRating = userEp?.userRating,
                        playAction = ActionPreparePlayer(
                            key = key,
                            title = ep.name.ifBlank { i18n.t("details.episode_title_fmt", ep.episodeNumber) },
                            targetSeason = seasonNumber,
                            targetEpisode = ep.episodeNumber
                        ),
                        toggleWatchedAction = ToggleEpisodeWatchedCommand(
                            key,
                            seasonNumber,
                            ep.episodeNumber,
                            !isWatched
                        )
                    )
                }
                content.copy(episodes = mappedEpisodes)
            }

            val seasons = state.metadata?.seasons ?: emptyList()
            logger.d { "[buildTvSeasonsFlow] mediaId=${key.id}, isLoading=${state.isLoading}, seasons.size=${seasons.size}, key.type=${key.type}" }
            if (state.isLoading && seasons.isEmpty()) {
                logger.d { "[buildTvSeasonsFlow] Emitting isLoading = true, empty seasons" }
                SlotUpdate(slotId = SlotId.TvSeasons, nodeId = "${pluginId}_seasons", state = SlotState.Loading())
            } else if (seasons.isEmpty() || key.type != EntityType.TV) {
                logger.d { "[buildTvSeasonsFlow] Emitting Empty (seasons empty or not TV)" }
                SlotUpdate(slotId = SlotId.TvSeasons, nodeId = "${pluginId}_seasons", state = SlotState.Empty)
            } else {
                val items = seasons.map { s ->
                    val watchedCount = userEpisodes.count { it.season == s.seasonNumber && it.isWatched }
                    val isFullyWatched = watchedCount > 0 && watchedCount >= s.episodeCount
                    val isWatching = watchedCount > 0 && watchedCount < s.episodeCount

                    SeasonItem(
                        id = s.id,
                        seasonNumber = s.seasonNumber,
                        name = s.name,
                        episodeCount = s.episodeCount,
                        isFullyWatched = isFullyWatched,
                        isWatching = isWatching,
                        selectAction = SelectSeasonCommand(key, s.seasonNumber),
                        markWatchedAction = MarkSeasonWatchedCommand(key, s.seasonNumber, !isFullyWatched)
                    )
                }

                SlotUpdate(
                    slotId = SlotId.TvSeasons,
                    nodeId = "${pluginId}_seasons",
                    state = if (state.isLoading) SlotState.Loading() else SlotState.Content(
                        SlotData.TvSeasons(
                            seasons = items,
                            selectedSeasonNumber = state.selectedSeasonNumber,
                            seasonContents = mappedSeasonContents
                        )
                    )
                )
            }
        }
        .distinctUntilChanged()
        .onStart {
            if (key.type == EntityType.TV) {
                emit(SlotUpdate(slotId = SlotId.TvSeasons, nodeId = "${pluginId}_seasons", state = SlotState.Loading()))
            }
        }
    }

    fun buildRecommendationsFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getRecommendationsState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED
                val untitled = i18n.t("details.untitled")
                val items = state.movies.map { it.toCarouselItem(untitled, titleMode) }
                if (state.isLoading && items.isEmpty()) {
                    val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                    SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_recs", state = SlotState.Loading())
                } else if (items.isEmpty()) {
                    SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_recs", state = SlotState.Empty)
                } else {
                    val loadMoreAction = if (!state.isLoading && state.error == null) LoadMoreRecommendations(
                        key,
                        state.page + 1
                    ) else null
                    val recsTitle = i18n.t("details.recommendations")
                    val titleAction = ActionNavigate(Screen.MediaList(key, "recommendations", recsTitle))
                    SlotUpdate(
                        slotId = SlotId.Carousels,
                        nodeId = "${pluginId}_recs",
                        state = if (state.isLoading) SlotState.Loading() else SlotState.Content(
                            SlotData.Carousel(
                                id = "recs_${key.id}",
                                title = recsTitle,
                                items = items,
                                loadMoreAction = loadMoreAction,
                                titleAction = titleAction,
                                telemetryContext = org.ensodai.avalonmediacard.contract.model.ClickstreamContext.CAROUSEL_DETAILS_RECOMMENDATIONS
                            )
                        )
                    )
                }
            }
            .onStart {
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val targetLang = userSettings.resolveTargetLanguage()
                val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                emit(SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_recs", state = SlotState.Loading()))
                stateManager.loadRecommendationsInitial(key, language = targetLang)
            }
    }

    fun buildSimilarFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getSimilarState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED
                val untitled = i18n.t("details.untitled")
                val items = state.movies.map { it.toCarouselItem(untitled, titleMode) }
                val title = if (key.type == EntityType.TV) i18n.t("details.similar_tv") else i18n.t("details.similar_movies")
                if (state.isLoading && items.isEmpty()) {
                    val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                    SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_similar", state = SlotState.Loading())
                } else if (items.isEmpty()) {
                    SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_similar", state = SlotState.Empty)
                } else {
                    val loadMoreAction =
                        if (!state.isLoading && state.error == null) LoadMoreSimilar(key, state.page + 1) else null
                    val titleAction = ActionNavigate(Screen.MediaList(key, "similar", title))
                    SlotUpdate(
                        slotId = SlotId.Carousels,
                        nodeId = "${pluginId}_similar",
                        state = if (state.isLoading) SlotState.Loading() else SlotState.Content(
                            SlotData.Carousel(
                                id = "similar_${key.id}",
                                title = title,
                                items = items,
                                loadMoreAction = loadMoreAction,
                                titleAction = titleAction,
                                telemetryContext = ClickstreamContext.CAROUSEL_DETAILS_SIMILAR
                            )
                        )
                    )
                }
            }
            .onStart {
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val targetLang = userSettings.resolveTargetLanguage()
                val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                emit(SlotUpdate(slotId = SlotId.Carousels, nodeId = "${pluginId}_similar", state = SlotState.Loading()))
                stateManager.loadSimilarInitial(key, language = targetLang)
            }
    }

    private fun TmdbMovieDto.toCarouselItem(
        fallbackTitle: String = "Untitled",
        titleMode: TitleDisplayMode = TitleDisplayMode.LOCALIZED
    ): MovieCarouselItem {
        val posterUrl = posterPath.toProxyImageUrl("w342")
        val type = if (title != null) EntityType.MOVIE else EntityType.TV
        val effectiveTitle = displayTitle(titleMode).ifBlank { fallbackTitle }
        return MovieCarouselItem(
            key = MediaKey(MediaProvider.Tmdb, type, id.toString()),
            title = effectiveTitle,
            posterUrl = posterUrl
        )
    }

    fun buildRecommendationsGridFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getRecommendationsState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED
                val untitled = i18n.t("details.untitled")
                val items = state.movies.map { it.toCarouselItem(untitled, titleMode) }
                val loadMoreAction =
                    if (!state.isLoading && state.error == null) LoadMoreRecommendations(key, state.page + 1) else null
                val gridData = SlotData.Grid(
                    id = "recs_grid_${key.id}",
                    items = items,
                    loadMoreAction = loadMoreAction
                )
                SlotUpdate(
                    slotId = SlotId.MediaList,
                    nodeId = "${pluginId}_recs_grid",
                    state = if (state.isLoading && items.isEmpty()) {
                        SlotState.Loading()
                    } else if (items.isEmpty()) {
                        SlotState.Empty
                    } else {
                        SlotState.Content(gridData)
                    }
                )
            }
            .onStart {
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val targetLang = userSettings.resolveTargetLanguage()
                val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                val gridData = SlotData.Grid(id = "recs_grid_${key.id}", items = dummyItems, loadMoreAction = null)
                emit(
                    SlotUpdate(
                        slotId = SlotId.MediaList,
                        nodeId = "${pluginId}_recs_grid",
                        state = SlotState.Loading()
                    )
                )
                stateManager.loadRecommendationsInitial(key, language = targetLang)
            }
    }

    fun buildSimilarGridFlow(
        key: MediaKey,
        userId: Uuid? = null,
        userSettingsProvider: UserGlobalSettingsProvider? = null
    ): Flow<SlotUpdate> {
        return stateManager.getSimilarState(key)
            .map { state ->
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED
                val untitled = i18n.t("details.untitled")
                val items = state.movies.map { it.toCarouselItem(untitled, titleMode) }
                val loadMoreAction =
                    if (!state.isLoading && state.error == null) LoadMoreSimilar(key, state.page + 1) else null
                val gridData = SlotData.Grid(
                    id = "similar_grid_${key.id}",
                    items = items,
                    loadMoreAction = loadMoreAction
                )
                SlotUpdate(
                    slotId = SlotId.MediaList,
                    nodeId = "${pluginId}_similar_grid",
                    state = if (state.isLoading && items.isEmpty()) {
                        SlotState.Loading()
                    } else if (items.isEmpty()) {
                        SlotState.Empty
                    } else {
                        SlotState.Content(gridData)
                    }
                )
            }
            .onStart {
                val userSettings = if (userId != null && userSettingsProvider != null) {
                    runCatching { userSettingsProvider.getUserSettings(userId) }.getOrNull()
                } else null
                val targetLang = userSettings.resolveTargetLanguage()
                val dummyItems = (1..5).map { MovieCarouselItem(key, "") }
                val gridData = SlotData.Grid(id = "similar_grid_${key.id}", items = dummyItems, loadMoreAction = null)
                emit(
                    SlotUpdate(
                        slotId = SlotId.MediaList,
                        nodeId = "${pluginId}_similar_grid",
                        state = SlotState.Loading()
                    )
                )
                stateManager.loadSimilarInitial(key, language = targetLang)
            }
    }
}
