package org.ensodai.avalonmediacard.plugins.homefeed

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.SidebarItemType
import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.resolveTargetLanguage
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class LoadMoreDynamicSection(val sectionId: String, val page: Int) : ServerAction

@Serializable
data class RetryLoadDynamicSection(val sectionId: String) : ServerAction

@Serializable
data class LoadMoreSearchPageCommand(val page: Int) : ServerAction

sealed interface FeedSectionState {
    data object Loading : FeedSectionState
    data class Success(val movies: List<TmdbMovieDto>) : FeedSectionState
    data class Error(val message: String) : FeedSectionState
}

class HomeFeedPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.homefeed"
    override val name: String = "Главный экран и Оркестрация"
    override val version: String = "2.0.0"
    override val author: String = "Antigravity"

    private val dynamicSectionStatesMap =
        java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<FeedSectionState>>()
    private val dynamicSectionPagesMap = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<Int>>()
    private val activeDynamicSections = java.util.concurrent.ConcurrentHashMap<String, DynamicSection>()

    data class SearchRequest(
        val query: String = "",
        val page: Int = 1,
        val isLoadMore: Boolean = false
    )

    private val userSearchRequests = java.util.concurrent.ConcurrentHashMap<kotlin.uuid.Uuid, kotlinx.coroutines.flow.MutableStateFlow<SearchRequest>>()
    private val userSearchItems = java.util.concurrent.ConcurrentHashMap<kotlin.uuid.Uuid, List<MovieCarouselItem>>()

    private fun getDynamicSectionState(
        sectionId: String,
        userId: kotlin.uuid.Uuid?,
        language: String = "ru"
    ): MutableStateFlow<FeedSectionState> {
        val normLang = language.lowercase().substringBefore("-").substringBefore("_")
        return dynamicSectionStatesMap.getOrPut("${sectionId}_${userId}_$normLang") { MutableStateFlow(FeedSectionState.Loading) }
    }

    private fun getDynamicSectionPage(
        sectionId: String,
        userId: kotlin.uuid.Uuid?,
        language: String = "ru"
    ): MutableStateFlow<Int> {
        val normLang = language.lowercase().substringBefore("-").substringBefore("_")
        return dynamicSectionPagesMap.getOrPut("${sectionId}_${userId}_$normLang") { MutableStateFlow(1) }
    }

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(LoadMoreDynamicSection::class)
            subclass(RetryLoadDynamicSection::class)
            subclass(LoadMoreSearchPageCommand::class)
        }
        polymorphic(ServerAction::class) {
            subclass(LoadMoreDynamicSection::class)
            subclass(RetryLoadDynamicSection::class)
            subclass(LoadMoreSearchPageCommand::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        context.sidebars.onSidebar { _ ->
            flow {
                emit(
                    listOf(
                        SidebarItem(
                            itemId = "home",
                            title = context.i18n.t("sidebar.home"),
                            iconName = "home",
                            screen = Screen.Dashboard,
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 0
                        ),
                        SidebarItem(
                            itemId = "movies",
                            title = context.i18n.t("sidebar.movies"),
                            iconName = "movie",
                            screen = Screen.Movies,
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 1
                        ),
                        SidebarItem(
                            itemId = "tv_shows",
                            title = context.i18n.t("sidebar.tv_shows"),
                            iconName = "tv",
                            screen = Screen.TvShows,
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 2
                        ),
                        SidebarItem(
                            itemId = "trends",
                            title = context.i18n.t("sidebar.trends"),
                            iconName = "flame",
                            screen = Screen.Trends,
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 3
                        ),
                        SidebarItem(
                            itemId = "search",
                            title = context.i18n.t("sidebar.search"),
                            iconName = "search",
                            screen = Screen.Search(""),
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 4
                        )
                    )
                )
            }
        }

        context.slots.declare<Screen.Dashboard>(
            slots = listOf(SlotId.HeroBanner, SlotId.CarouselBackdrops, SlotId.Exploration, SlotId.Carousels, SlotId.Banner),
            manifestLayout = { userId -> buildManifestLayout(context, userId, "dashboard") }
        )
        context.slots.onScreen<Screen.Dashboard> { _, userId ->
            buildTabSlots(context, userId, "dashboard")
        }

        context.slots.declare<Screen.Movies>(
            slots = listOf(SlotId.HeroBanner, SlotId.Carousels, SlotId.Banner),
            manifestLayout = { userId -> buildManifestLayout(context, userId, "movies") }
        )
        context.slots.onScreen<Screen.Movies> { _, userId ->
            buildTabSlots(context, userId, "movies")
        }

        context.slots.declare<Screen.TvShows>(
            slots = listOf(SlotId.HeroBanner, SlotId.Carousels, SlotId.Banner),
            manifestLayout = { userId -> buildManifestLayout(context, userId, "tv_shows") }
        )
        context.slots.onScreen<Screen.TvShows> { _, userId ->
            buildTabSlots(context, userId, "tv_shows")
        }

        context.slots.declare<Screen.Trends>(
            slots = listOf(SlotId.HeroBanner, SlotId.Carousels, SlotId.Banner),
            manifestLayout = { userId -> buildManifestLayout(context, userId, "trends") }
        )
        context.slots.onScreen<Screen.Trends> { _, userId ->
            buildTabSlots(context, userId, "trends")
        }

        context.slots.declare<Screen.Search>(
            slots = listOf(SlotId.SearchResults),
            manifestLayout = {
                listOf(LayoutNode("search_results", SlotId.SearchResults))
            }
        )
        context.slots.onScreen<Screen.Search> { screen, userId ->
            if (userId == null) return@onScreen ScreenSlots(emptyList(), emptyFlow())
            
            val queryFlow = userSearchRequests.getOrPut(userId) { MutableStateFlow(SearchRequest(screen.initialQuery)) }
            if (queryFlow.value.query != screen.initialQuery) {
                queryFlow.value = SearchRequest(query = screen.initialQuery, page = 1, isLoadMore = false)
            }

            val flow = queryFlow.asStateFlow()
                .transformLatest { request ->
                    if (request.query.isBlank()) {
                        userSearchItems[userId] = emptyList()
                        emit(
                            ScreenStreamEvent.Update(
                                SlotUpdate(
                                    slotId = SlotId.SearchResults,
                                    nodeId = "search_results",
                                    state = SlotState.Content(
                                        SlotData.Grid(
                                            id = "search_results",
                                            title = context.i18n.t("search.placeholder"),
                                            items = emptyList()
                                        )
                                    )
                                )
                            )
                        )
                    } else {
                        if (!request.isLoadMore) {
                            userSearchItems[userId] = emptyList()
                            emit(
                                ScreenStreamEvent.Update(
                                    SlotUpdate(
                                        slotId = SlotId.SearchResults,
                                        nodeId = "search_results",
                                        state = SlotState.Loading()
                                    )
                                )
                            )
                        }

                        try {
                            val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
                            val targetLang = userSettings.resolveTargetLanguage()
                            val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED

                            val results = context.catalog.searchMedia(request.query, request.page, targetLang)
                            val tvText = context.i18n.t("media.tv")
                            val movieText = context.i18n.t("media.movie")
                            val mediaText = context.i18n.t("media.media")

                            val newItems = results.map { dto ->
                                val typeBadge = when {
                                    dto.mediaType == "tv" -> tvText
                                    dto.mediaType == "movie" -> movieText
                                    else -> mediaText
                                }
                                MovieCarouselItem(
                                    key = MediaKey(
                                        MediaProvider.Tmdb,
                                        if (dto.mediaType == "tv") EntityType.TV else EntityType.MOVIE,
                                        dto.id.toString()
                                    ),
                                    title = dto.displayTitle(titleMode).ifBlank { dto.title ?: dto.name ?: "" },
                                    posterUrl = dto.posterPath.toProxyImageUrl("w500"),
                                    badges = listOf(typeBadge)
                                )
                            }
                            
                            val accumulatedItems = if (request.isLoadMore) {
                                (userSearchItems[userId] ?: emptyList()) + newItems
                            } else {
                                newItems
                            }

                            userSearchItems[userId] = accumulatedItems
                            
                            val loadMoreAction = if (newItems.isNotEmpty()) {
                                LoadMoreSearchPageCommand(request.page + 1)
                            } else {
                                null
                            }

                            emit(
                                ScreenStreamEvent.Update(
                                    SlotUpdate(
                                        slotId = SlotId.SearchResults,
                                        nodeId = "search_results",
                                        state = SlotState.Content(
                                            SlotData.Grid(
                                                id = "search_results",
                                                title = context.i18n.t("search.results"),
                                                items = accumulatedItems,
                                                loadMoreAction = loadMoreAction
                                            )
                                        )
                                    )
                                )
                            )
                            
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emit(
                                ScreenStreamEvent.Update(
                                    SlotUpdate(
                                        slotId = SlotId.SearchResults,
                                        nodeId = "search_results",
                                        state = SlotState.Error(context.i18n.t("search.error"))
                                    )
                                )
                            )
                        }
                    }
                }

            ScreenSlots(
                layout = listOf(
                    LayoutNode("search_results", SlotId.SearchResults)
                ),
                flow = flow
            )
        }

        context.actions.bind<SearchQueryCommand> { cmd, userId ->
            if (userId != null) {
                userSearchRequests[userId]?.value = SearchRequest(query = cmd.query, page = 1, isLoadMore = false)
            }
            ActionResult.NoOp
        }

        context.actions.bind<LoadMoreSearchPageCommand> { cmd, userId ->
            if (userId != null) {
                val currentState = userSearchRequests[userId]?.value
                if (currentState != null && currentState.query.isNotBlank()) {
                    userSearchRequests[userId]?.value = currentState.copy(page = cmd.page, isLoadMore = true)
                }
            }
            ActionResult.NoOp
        }

        context.actions.bind<LoadMoreDynamicSection> { cmd, userId ->
            if (userId != null) {
                val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
                val targetLang = userSettings.resolveTargetLanguage()
                val section = activeDynamicSections["${cmd.sectionId}_${userId}_$targetLang"]
                    ?: activeDynamicSections["${cmd.sectionId}_$userId"]
                if (section != null) {
                    loadDynamicSectionPage(section, cmd.page, context, userId, targetLang)
                }
            }
            ActionResult.NoOp
        }

        context.actions.bind<RetryLoadDynamicSection> { cmd, userId ->
            if (userId != null) {
                val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
                val targetLang = userSettings.resolveTargetLanguage()
                val section = activeDynamicSections["${cmd.sectionId}_${userId}_$targetLang"]
                    ?: activeDynamicSections["${cmd.sectionId}_$userId"]
                if (section != null) {
                    val flow = getDynamicSectionState(cmd.sectionId, userId, targetLang)
                    flow.value = FeedSectionState.Loading
                    loadDynamicSectionInitial(section, context, userId, targetLang)
                }
            }
            ActionResult.NoOp
        }
    }

    private fun loadDynamicSectionInitial(section: DynamicSection, context: PluginContext, userId: kotlin.uuid.Uuid?, language: String) {
        val flow = getDynamicSectionState(section.id, userId, language)
        val pageFlow = getDynamicSectionPage(section.id, userId, language)
        if (flow.value !is FeedSectionState.Loading && flow.value !is FeedSectionState.Error) return

        flow.value = FeedSectionState.Loading
        pageFlow.value = 1

        context.scope.launch {
            try {
                val movies = fetchDynamicMovies(section, 1, context, language)
                flow.value = FeedSectionState.Success(movies)
            } catch (e: Exception) {
                flow.value = FeedSectionState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private fun loadDynamicSectionPage(
        section: DynamicSection,
        page: Int,
        context: PluginContext,
        userId: Uuid?,
        language: String
    ) {
        if (section.mediaIds.isNotEmpty()) return

        val flow = getDynamicSectionState(section.id, userId, language)
        val currentState = flow.value as? FeedSectionState.Success ?: return
        val currentMovies = currentState.movies

        context.scope.launch {
            try {
                val newMovies = fetchDynamicMovies(section, page, context, language)
                if (newMovies.isNotEmpty()) {
                    val combined = (currentMovies + newMovies).distinctBy { it.id }
                    flow.value = FeedSectionState.Success(combined)
                    getDynamicSectionPage(section.id, userId, language).value = page
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getOrGenerateSections(
        context: PluginContext,
        userId: Uuid,
        scopeName: String,
        language: String
    ): List<DynamicSection> {
        val dbSections = context.feedCache.getSections(userId, scopeName, language)
        if (dbSections != null && dbSections.isNotEmpty()) {
            return dbSections
        }

        val engine = context.recommendations as? RecommendationEngine ?: return emptyList()
        val generated = try {
            if (scopeName == "dashboard") engine.generateDashboard(userId, language)
            else engine.generateTab(userId, scopeName, language)
        } catch (e: Exception) {
            context.logger.error("Failed to generate sections for tab $scopeName and user $userId", e)
            emptyList()
        }

        if (generated.isNotEmpty()) {
            context.feedCache.saveSections(userId, scopeName, language, generated)
        }
        return generated
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun buildTabSlots(
        context: PluginContext,
        userId: Uuid?,
        scopeName: String
    ): ScreenSlots {
        if (userId == null) return ScreenSlots(emptyList(), emptyFlow())

        val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
        val targetLang = userSettings.resolveTargetLanguage()
        val titleMode = userSettings?.titleMode ?: TitleDisplayMode.LOCALIZED

        val tmdbTokenSetting = runCatching { context.integrationManager.getTmdbToken(userId) }.getOrNull()
        val isTmdbConfigured = tmdbTokenSetting != null
        val globalTokenSetting = runCatching { context.integrationManager.getTmdbToken(null) }.getOrNull()
        val isFreshServerWithoutToken = globalTokenSetting == null

        if (!isTmdbConfigured) {
            val title = if (isFreshServerWithoutToken) {
                context.i18n.t("onboarding.admin.title")
            } else {
                context.i18n.t("onboarding.user.title")
            }
            val desc = if (isFreshServerWithoutToken) {
                context.i18n.t("onboarding.admin.desc")
            } else {
                context.i18n.t("onboarding.user.desc")
            }
            val buttonLabel = if (isFreshServerWithoutToken) {
                context.i18n.t("onboarding.admin.button")
            } else {
                context.i18n.t("onboarding.user.button")
            }
            val action = if (isFreshServerWithoutToken) ActionNavigate(Screen.Admin) else ActionNavigate(Screen.Settings)

            val bannerNode = LayoutNode("onboarding_banner", SlotId.Banner)
            val bannerSlotUpdate = SlotUpdate(
                SlotId.Banner,
                "onboarding_banner",
                SlotState.Content(
                    SlotData.Banner(
                        id = "onboarding_banner",
                        title = title,
                        description = desc,
                        iconName = "settings",
                        primaryAction = action,
                        primaryActionLabel = buttonLabel
                    )
                )
            )
            return ScreenSlots(
                layout = listOf(bannerNode),
                flow = flowOf(ScreenStreamEvent.Update(bannerSlotUpdate))
            )
        }

        val dynamicSections = getOrGenerateSections(context, userId, scopeName, targetLang)
        val heroSections = dynamicSections.filter { it.type == SectionType.HERO }
        val allOtherCandidateSections = dynamicSections.filter { it.type != SectionType.HERO }

        val flow = channelFlow {
            val activeCandidates = java.util.concurrent.CopyOnWriteArrayList(allOtherCandidateSections)
            val candidateQueue = java.util.concurrent.CopyOnWriteArrayList<DynamicSection>()

            fun emitLayout() {
                val layoutNodes = mutableListOf<LayoutNode>()
                if (heroSections.isNotEmpty()) {
                    layoutNodes.add(LayoutNode("hero_merged", SlotId.HeroBanner))
                }
                activeCandidates.forEach { section ->
                    val slotId = when (section.type) {
                        SectionType.CAROUSEL_BACKDROPS -> SlotId.CarouselBackdrops
                        SectionType.EXPLORATION -> SlotId.Exploration
                        else -> SlotId.Carousels
                    }
                    layoutNodes.add(LayoutNode(section.id, slotId))
                }
                launch { send(ScreenStreamEvent.Layout(layoutNodes)) }
            }

            emitLayout()

            val primaryHero = heroSections.firstOrNull()
            if (primaryHero != null) {
                launch {
                    try {
                        val hMovies = fetchDynamicMovies(primaryHero, 1, context, targetLang).distinctBy { it.id }.take(5)
                        if (hMovies.isNotEmpty()) {
                            val items = hMovies.map { it.toCarouselItem(titleMode) }
                            send(
                                ScreenStreamEvent.Update(
                                    SlotUpdate(
                                        SlotId.HeroBanner, "hero_merged",
                                        SlotState.Content(
                                            SlotData.Hero(
                                                id = "hero_merged",
                                                title = primaryHero.title,
                                                subtitle = primaryHero.description,
                                                items = items,
                                                telemetryContext = ClickstreamContext.CAROUSEL_DISCOVER
                                            )
                                        )
                                    )
                                )
                            )
                        } else {
                            send(ScreenStreamEvent.Update(SlotUpdate(SlotId.HeroBanner, "hero_merged", SlotState.Empty)))
                        }
                    } catch (e: Exception) {
                        send(ScreenStreamEvent.Update(SlotUpdate(SlotId.HeroBanner, "hero_merged", SlotState.Empty)))
                    }
                }
            } else {
                send(ScreenStreamEvent.Update(SlotUpdate(SlotId.HeroBanner, "hero_merged", SlotState.Empty)))
            }

            fun launchCandidate(section: DynamicSection) {
                launch {
                    activeDynamicSections["${section.id}_${userId}_$targetLang"] = section
                    val stateFlow = getDynamicSectionState(section.id, userId, targetLang)
                    val pageFlow = getDynamicSectionPage(section.id, userId, targetLang)

                    combine(stateFlow, pageFlow) { state, page ->
                        val sectionSlotId = when (section.type) {
                            SectionType.CAROUSEL_BACKDROPS -> SlotId.CarouselBackdrops
                            SectionType.EXPLORATION -> SlotId.Exploration
                            else -> SlotId.Carousels
                        }
                        when (state) {
                            is FeedSectionState.Loading -> SlotUpdate(sectionSlotId, section.id, SlotState.Loading())
                            is FeedSectionState.Error -> SlotUpdate(
                                sectionSlotId,
                                section.id,
                                SlotState.Error(
                                    message = state.message,
                                    retryAction = RetryLoadDynamicSection(section.id)
                                )
                            )

                            is FeedSectionState.Success -> {
                                val items = state.movies.map { it.toCarouselItem(titleMode) }
                                if (page == 1 && items.isEmpty()) {
                                    synchronized(activeCandidates) {
                                        val index = activeCandidates.indexOfFirst { it.id == section.id }
                                        if (index != -1) {
                                            activeCandidates.removeAt(index)
                                            val nextReplacement =
                                                if (candidateQueue.isNotEmpty()) candidateQueue.removeAt(0) else null
                                            if (nextReplacement != null) {
                                                activeCandidates.add(index, nextReplacement)
                                                emitLayout()
                                                launchCandidate(nextReplacement)
                                            } else {
                                                emitLayout()
                                            }
                                        }
                                    }
                                    null
                                } else {
                                    val loadMore = if (section.mediaIds.isNotEmpty() || items.isEmpty()) null else LoadMoreDynamicSection(section.id, page + 1)
                                    val telemetry = ClickstreamContext.CAROUSEL_DISCOVER

                                    val slotData = when (section.type) {
                                        SectionType.CAROUSEL_BACKDROPS -> SlotData.CarouselBackdrops(
                                            section.id,
                                            section.title,
                                            items,
                                            loadMore,
                                            null,
                                            telemetry
                                        )

                                        SectionType.EXPLORATION -> SlotData.Exploration(
                                            section.id,
                                            section.title,
                                            items,
                                            loadMore,
                                            telemetry
                                        )

                                        else -> SlotData.Carousel(
                                            section.id,
                                            section.title,
                                            items,
                                            loadMore,
                                            null,
                                            telemetry
                                        )
                                    }
                                    SlotUpdate(sectionSlotId, section.id, SlotState.Content(slotData))
                                }
                            }
                        }
                    }
                        .filterNotNull()
                        .onStart { loadDynamicSectionInitial(section, context, userId, targetLang) }
                        .collect { update ->
                            send(ScreenStreamEvent.Update(update))
                        }
                }
            }

            activeCandidates.forEach { candidate ->
                launchCandidate(candidate)
            }
        }

        val initialLayoutNodes = buildManifestLayout(context, userId, scopeName)

        return ScreenSlots(
            layout = initialLayoutNodes,
            flow = flow
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun buildManifestLayout(
        context: PluginContext,
        userId: Uuid?,
        scopeName: String
    ): List<LayoutNode> {
        if (userId == null) return emptyList()

        val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
        val targetLang = userSettings.resolveTargetLanguage()

        val tmdbTokenSetting = runCatching { context.integrationManager.getTmdbToken(userId) }.getOrNull()
        val isTmdbConfigured = tmdbTokenSetting != null

        if (!isTmdbConfigured) {
            return listOf(LayoutNode("onboarding_banner", SlotId.Banner))
        }

        val dynamicSections = getOrGenerateSections(context, userId, scopeName, targetLang)
        val heroSections = dynamicSections.filter { it.type == SectionType.HERO }
        val allOtherCandidateSections = dynamicSections.filter { it.type != SectionType.HERO }

        val initialLayoutNodes = mutableListOf<LayoutNode>()
        if (heroSections.isNotEmpty()) {
            initialLayoutNodes.add(LayoutNode("hero_merged", SlotId.HeroBanner))
        }
        allOtherCandidateSections.forEach { section ->
            val slotId = when (section.type) {
                SectionType.CAROUSEL_BACKDROPS -> SlotId.CarouselBackdrops
                SectionType.EXPLORATION -> SlotId.Exploration
                else -> SlotId.Carousels
            }
            initialLayoutNodes.add(LayoutNode(section.id, slotId))
        }

        return initialLayoutNodes
    }

    private suspend fun fetchDynamicMovies(
        section: DynamicSection,
        page: Int,
        context: PluginContext,
        language: String
    ): List<TmdbMovieDto> {
        if (section.mediaIds.isNotEmpty()) {
            if (page > 1) return emptyList()
            val batchMap = try {
                context.catalog.getMediaDetailsBatch(section.mediaIds, requireSeasons = false, requireVideos = false, language = language)
            } catch (e: Exception) {
                emptyMap()
            }
            return section.mediaIds.mapNotNull { key ->
                val details = batchMap[key] ?: return@mapNotNull null
                TmdbMovieDto(
                    id = key.id.substringAfterLast(":").toIntOrNull() ?: 0,
                    title = details.title,
                    name = details.title,
                    originalTitle = details.originalTitle,
                    originalName = details.originalTitle,
                    posterPath = details.posterUrl,
                    backdropPath = details.backgroundUrl,
                    releaseDate = details.releaseDate,
                    voteAverage = details.rating?.toDoubleOrNull(),
                    overview = details.description
                )
            }
        }

        var attempts = 0
        while (true) {
            try {
                return context.catalog.discoverMediaByParams(section.queryParams, section.targetType, page, language)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                attempts++
                if (attempts >= 3) throw e
                delay((1000L * attempts).milliseconds)
            }
        }
    }

    private fun TmdbMovieDto.toCarouselItem(titleMode: TitleDisplayMode = TitleDisplayMode.LOCALIZED): MovieCarouselItem {
        val poster = posterPath.toProxyImageUrl("w342")
        val backdrop = backdropPath.toProxyImageUrl("w1280")
        val rawTitle = displayTitle(titleMode).ifBlank { title ?: name ?: "Без названия" }
        return MovieCarouselItem(
            key = MediaKey(MediaProvider.Tmdb, if (title != null) EntityType.MOVIE else EntityType.TV, id.toString()),
            title = rawTitle,
            posterUrl = poster,
            backdropUrl = backdrop
        )
    }
}
