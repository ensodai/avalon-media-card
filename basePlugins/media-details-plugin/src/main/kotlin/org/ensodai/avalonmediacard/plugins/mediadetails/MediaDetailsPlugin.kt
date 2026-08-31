package org.ensodai.avalonmediacard.plugins.mediadetails

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.resolveTargetLanguage
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.plugins.mediadetails.domain.*
import org.ensodai.avalonmediacard.plugins.mediadetails.presentation.MediaDetailsSlotFactory
import org.ensodai.avalonmediacard.plugins.mediadetails.presentation.MediaDetailsStateManager
import org.ensodai.avalonmediacard.plugins.mediadetails.useractions.*
import kotlin.uuid.Uuid

class MediaDetailsPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.mediadetails"
    override val name: String = "Детали Медиа и Виджеты"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(ToggleCollectionCommand::class)
            subclass(SetRatingCommand::class)
            subclass(SetStatusCommand::class)
            subclass(ToggleCustomListCommand::class)
            subclass(CreateCustomListCommand::class)
            subclass(LoadMoreRecommendations::class)
            subclass(LoadMoreSimilar::class)
            subclass(SelectSeasonCommand::class)
            subclass(MarkSeasonWatchedCommand::class)
            subclass(ToggleEpisodeWatchedCommand::class)
            subclass(RateEpisodeCommand::class)
            subclass(RetryLoadMediaDetailsCommand::class)
        }
        polymorphic(ServerAction::class) {
            subclass(ToggleCollectionCommand::class)
            subclass(SetRatingCommand::class)
            subclass(SetStatusCommand::class)
            subclass(ToggleCustomListCommand::class)
            subclass(CreateCustomListCommand::class)
            subclass(LoadMoreRecommendations::class)
            subclass(LoadMoreSimilar::class)
            subclass(SelectSeasonCommand::class)
            subclass(MarkSeasonWatchedCommand::class)
            subclass(ToggleEpisodeWatchedCommand::class)
            subclass(RateEpisodeCommand::class)
            subclass(RetryLoadMediaDetailsCommand::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        val repository = MediaDetailsRepositoryImpl(context)

        val stateManager = MediaDetailsStateManager(
            scope = context.scope,
            getMediaDetailsUseCase = GetMediaDetailsUseCase(repository),
            getRecommendationsUseCase = GetRecommendationsUseCase(repository),
            getSimilarUseCase = GetSimilarUseCase(repository),
            getSeasonDetailsUseCase = GetSeasonDetailsUseCase(repository)
        )

        val slotFactory = MediaDetailsSlotFactory(
            pluginId = id,
            stateManager = stateManager,
            i18n = context.i18n,
            genreDictionaryProvider = context.genreDictionary
        )

        val userActionsStateManager = UserActionsStateManager(
            scope = context.scope,
            userMovieProvider = context.userMovies,
            userCustomListProvider = context.userCustomLists,
            catalog = context.catalog
        )

        val userActionsSlotFactory = UserActionsSlotFactory(
            pluginId = id,
            stateManager = userActionsStateManager,
            i18n = context.i18n
        )

        fun buildPromises(
            key: MediaKey,
            userId: Uuid?,
            isTvShow: Boolean
        ): org.ensodai.avalonmediacard.contract.plugins.ScreenSlots {
            val promises = mutableMapOf<SlotId, Flow<SlotUpdate>>()
            val layout = mutableListOf<org.ensodai.avalonmediacard.contract.slot.LayoutNode>()

            fun add(slotId: SlotId, nodeId: String, flow: Flow<SlotUpdate>) {
                promises[slotId] = flow
                layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(nodeId, slotId))
            }

            add(SlotId.Header, id, slotFactory.buildHeaderFlow(key, userId, context.userGlobalSettings))
            add(SlotId.Description, id, slotFactory.buildDescriptionFlow(key, userId, context.userGlobalSettings))
            add(SlotId.Cast, id, slotFactory.buildCastFlow(key, userId, context.userGlobalSettings))

            // Carousels
            promises[SlotId.Carousels] = merge(
                slotFactory.buildRecommendationsFlow(key, userId, context.userGlobalSettings),
                slotFactory.buildSimilarFlow(key, userId, context.userGlobalSettings)
            )
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode("${id}_recs", SlotId.Carousels))
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode("${id}_similar", SlotId.Carousels))

            if (isTvShow) {
                add(SlotId.TvSeasons, "${id}_seasons", slotFactory.buildTvSeasonsFlow(key, userId, context.userMovies, context.userGlobalSettings))
            }

            if (userId != null) {
                add(SlotId.CollectionButtons, id, userActionsSlotFactory.buildCollectionButtonsFlow(key, userId))
                add(SlotId.UserActions, id, userActionsSlotFactory.buildUserActionsFlow(key, userId))
            }
            return org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = layout,
                flow = merge(*promises.values.toTypedArray()).map { ScreenStreamEvent.Update(it) }
            )
        }

        val movieDetailsSlots = listOf(
            SlotId.Header, SlotId.Description, SlotId.Cast, SlotId.Carousels,
            SlotId.CollectionButtons, SlotId.UserActions
        )
        val tvDetailsSlots = movieDetailsSlots + SlotId.TvSeasons

        fun buildManifestLayout(isTvShow: Boolean, userId: Uuid?): List<org.ensodai.avalonmediacard.contract.slot.LayoutNode> {
            val layout = mutableListOf<org.ensodai.avalonmediacard.contract.slot.LayoutNode>()
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.Header))
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.Description))
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.Cast))
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode("${id}_recs", SlotId.Carousels))
            layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode("${id}_similar", SlotId.Carousels))
            if (isTvShow) {
                layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode("${id}_seasons", SlotId.TvSeasons))
            }
            if (userId != null) {
                layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.CollectionButtons))
                layout.add(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.UserActions))
            }
            return layout
        }

        context.slots.declare<Screen.MovieDetails>(movieDetailsSlots) { userId -> buildManifestLayout(false, userId) }
        context.slots.onScreen<Screen.MovieDetails> { screen, userId ->
            buildPromises(screen.key, userId, isTvShow = false)
        }

        context.slots.declare<Screen.TvShowDetails>(tvDetailsSlots) { userId -> buildManifestLayout(true, userId) }
        context.slots.onScreen<Screen.TvShowDetails> { screen, userId ->
            buildPromises(screen.key, userId, isTvShow = true)
        }

        context.slots.onScreen<Screen.MediaList> { screen, userId ->
            val key = screen.key
            val promises = mutableMapOf<SlotId, Flow<SlotUpdate>>()
            val layout = mutableListOf<org.ensodai.avalonmediacard.contract.slot.LayoutNode>()

            when (screen.listType) {
                "recommendations" -> {
                    promises[SlotId.MediaGrid] = slotFactory.buildRecommendationsGridFlow(key, userId, context.userGlobalSettings)
                    layout.add(
                        org.ensodai.avalonmediacard.contract.slot.LayoutNode(
                            "${id}_recs_grid",
                            SlotId.MediaGrid
                        )
                    )
                }

                "similar" -> {
                    promises[SlotId.MediaGrid] = slotFactory.buildSimilarGridFlow(key, userId, context.userGlobalSettings)
                    layout.add(
                        org.ensodai.avalonmediacard.contract.slot.LayoutNode(
                            "${id}_similar_grid",
                            SlotId.MediaGrid
                        )
                    )
                }
            }
            org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = layout,
                flow = merge(*promises.values.toTypedArray()).map { ScreenStreamEvent.Update(it) }
            )
        }

        // --- Commands ---
        context.actions.bind<ToggleCollectionCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.toggleCollection(userId, cmd.key, cmd.inCollection)
            }
            ActionResult.NoOp
        }

        context.actions.bind<SetRatingCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.setRating(userId, cmd.key, cmd.rating)
            }
            ActionResult.NoOp
        }

        context.actions.bind<SetStatusCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.setStatus(userId, cmd.key, cmd.status)
            }
            ActionResult.NoOp
        }

        context.actions.bind<ToggleCustomListCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.toggleCustomList(userId, cmd.key, cmd.listId)
            }
            ActionResult.NoOp
        }

        context.actions.bind<CreateCustomListCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.createCustomList(userId, cmd.key, cmd.listName)
            }
            ActionResult.NoOp
        }

        context.actions.bind<LoadMoreRecommendations> { cmd, userId ->
            val userSettings = if (userId != null) runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull() else null
            val targetLang = userSettings.resolveTargetLanguage()
            stateManager.loadMoreRecommendations(cmd.key, cmd.page, language = targetLang)
            ActionResult.NoOp
        }

        context.actions.bind<LoadMoreSimilar> { cmd, userId ->
            val userSettings = if (userId != null) runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull() else null
            val targetLang = userSettings.resolveTargetLanguage()
            stateManager.loadMoreSimilar(cmd.key, cmd.page, language = targetLang)
            ActionResult.NoOp
        }

        context.actions.bind<SelectSeasonCommand> { cmd, userId ->
            val userSettings = if (userId != null) runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull() else null
            val targetLang = userSettings.resolveTargetLanguage()
            stateManager.loadSeasonDetails(cmd.key, cmd.seasonNumber, language = targetLang)
            ActionResult.NoOp
        }

        context.actions.bind<MarkSeasonWatchedCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.markSeasonWatched(userId, cmd.key, cmd.seasonNumber, cmd.isWatched)
            }
            ActionResult.NoOp
        }

        context.actions.bind<ToggleEpisodeWatchedCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.toggleEpisodeWatched(
                    userId,
                    cmd.key,
                    cmd.seasonNumber,
                    cmd.episodeNumber,
                    cmd.isWatched
                )
            }
            ActionResult.NoOp
        }

        context.actions.bind<RateEpisodeCommand> { cmd, userId ->
            if (userId != null) {
                userActionsStateManager.rateEpisode(userId, cmd.key, cmd.seasonNumber, cmd.episodeNumber, cmd.rating)
            }
            ActionResult.NoOp
        }

        context.actions.bind<RetryLoadMediaDetailsCommand> { cmd, userId ->
            val userSettings = if (userId != null) runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull() else null
            val targetLang = userSettings.resolveTargetLanguage()
            stateManager.loadMediaDetailsInitial(cmd.key, force = true, language = targetLang)
            ActionResult.NoOp
        }
    }
}
