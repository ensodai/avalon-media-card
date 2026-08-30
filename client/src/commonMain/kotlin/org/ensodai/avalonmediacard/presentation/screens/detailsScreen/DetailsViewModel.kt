package org.ensodai.avalonmediacard.presentation.screens.detailsScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.SearchMediaSourcesUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.SelectMediaSourceUseCase
import org.ensodai.avalonmediacard.presentation.core.*
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

@KoinViewModel
class DetailsViewModel(
    @InjectedParam private val mediaKey: MediaKey,
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val selectMediaSource: SelectMediaSourceUseCase,
    private val searchMediaSources: SearchMediaSourcesUseCase,
    private val manifestRepository: GlobalManifestRepository,
    private val tokenStorage: TokenStorage
) : SduiViewModel<DetailsViewState>(DetailsViewState(mediaKey = mediaKey)) {

    private val logger = AppLogging.logger("DetailsViewModel")

    init {
        logger.d { "[PROFILING] DetailsViewModel INIT: ${Clock.System.now()}" }
        val isTv = mediaKey.type == EntityType.TV

        val screenName = if (isTv) "TvShowDetails" else "MovieDetails"
        val manifest = manifestRepository.getScreenManifest(screenName)
        val layout = manifest?.layout ?: emptyList()
        
        logger.d { "[MANIFEST_DUMP] $screenName Layout at Frame 0: $layout" }

        // Mirage state (Кадр 0) strictly based on Global Manifest Layout
        updateViewState {
            it.copy(
                header = layout.firstOrNull { node -> node.slotId == SlotId.Header }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                playButtons = layout.firstOrNull { node -> node.slotId == SlotId.PlayButtons }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                description = layout.firstOrNull { node -> node.slotId == SlotId.Description }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                cast = layout.firstOrNull { node -> node.slotId == SlotId.Cast }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                tvSeasons = layout.firstOrNull { node -> node.slotId == SlotId.TvSeasons }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                carousels = layout.filter { node -> node.slotId == SlotId.Carousels }
                    .map { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                comments = layout.firstOrNull { node -> node.slotId == SlotId.Comments }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                collectionButtons = layout.firstOrNull { node -> node.slotId == SlotId.CollectionButtons }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                userActions = layout.firstOrNull { node -> node.slotId == SlotId.UserActions }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                syncStatus = layout.firstOrNull { node -> node.slotId == SlotId.SyncStatus }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                mediaSourcesList = layout.filter { node -> node.slotId == SlotId.MediaSources }
                    .map { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
                torrentInspector = layout.firstOrNull { node -> node.slotId == SlotId.TorrentInspector }
                    ?.let { node -> SduiSlot(node.nodeId, SlotUiState(isLoading = true)) },
            )
        }
        loadSlots()
    }


    private fun loadSlots() {
        viewModelScope.launch {
            try {
                logger.d { "[PROFILING] DetailsViewModel start streamScreenSlots: ${Clock.System.now()}" }
                val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()
                streamScreenSlots(Screen.Details(mediaKey)).collect { event ->
                    when (event) {


                        is ScreenStreamEvent.Layout -> {
                            logger.d { "[LAYOUT_STREAM_DUMP] Received ScreenStreamEvent.Layout with nodes: ${event.nodes}" }
                        }
                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            val stateType = update.state::class.simpleName
                            logger.d { "[PROFILING] DetailsViewModel received SlotUpdate(${update.slotId}, $stateType): ${kotlin.time.Clock.System.now()}" }

                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState { current ->
                                when (update.slotId) {
                                    SlotId.Header -> current.copy(
                                        header = currentSlotsMap.extractSlot<SlotData.Header>(SlotId.Header, oldSlot = current.header)
                                    )
                                    SlotId.PlayButtons -> current.copy(
                                        playButtons = currentSlotsMap.extractSlot<SlotData.ButtonGroup>(SlotId.PlayButtons, oldSlot = current.playButtons)
                                    )
                                    SlotId.CollectionButtons -> current.copy(
                                        collectionButtons = currentSlotsMap.extractSlot<SlotData.ButtonGroup>(SlotId.CollectionButtons, oldSlot = current.collectionButtons)
                                    )
                                    SlotId.ContinueWatching -> current.copy(
                                        continueWatching = currentSlotsMap.extractSlot<SlotData.ContinueWatching>(SlotId.ContinueWatching, oldSlot = current.continueWatching)
                                    )
                                    SlotId.UserActions -> current.copy(
                                        userActions = currentSlotsMap.extractSlot<SlotData.UserActions>(SlotId.UserActions, oldSlot = current.userActions)
                                    )
                                    SlotId.SyncStatus -> current.copy(
                                        syncStatus = currentSlotsMap.extractSlot<SlotData.SyncStatus>(SlotId.SyncStatus, oldSlot = current.syncStatus)
                                    )
                                    SlotId.Description -> current.copy(
                                        description = currentSlotsMap.extractSlot<SlotData.Text>(SlotId.Description, oldSlot = current.description)
                                    )
                                    SlotId.TvSeasons -> current.copy(
                                        tvSeasons = currentSlotsMap.extractSlot<SlotData.TvSeasons>(SlotId.TvSeasons, oldSlot = current.tvSeasons)
                                    )
                                    SlotId.MediaSources -> current.copy(
                                        mediaSourcesList = currentSlotsMap.extractSlots<SlotData.MediaSources>(SlotId.MediaSources, oldSlots = current.mediaSourcesList)
                                    )
                                    SlotId.TorrentInspector -> current.copy(
                                        torrentInspector = currentSlotsMap.extractSlot<SlotData.TorrentInspector>(SlotId.TorrentInspector, oldSlot = current.torrentInspector)
                                    )
                                    SlotId.Cast -> current.copy(
                                        cast = currentSlotsMap.extractSlot<SlotData.Cast>(SlotId.Cast, oldSlot = current.cast)
                                    )
                                    SlotId.Carousels -> current.copy(
                                        carousels = currentSlotsMap.extractSlots<SlotData.Carousel>(SlotId.Carousels, oldSlots = current.carousels)
                                    )
                                    SlotId.Comments -> current.copy(
                                        comments = currentSlotsMap.extractSlot<SlotData.Comments>(SlotId.Comments, oldSlot = current.comments)
                                    )
                                    else -> current
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    private fun updatePlayingEpisode(
        season: Int,
        episode: Int,
        update: (MediaStream) -> MediaStream
    ) {
        val state = viewState.value.playerState
        if (state is DetailsViewState.PlayerState.Playing) {
            val updatedPlaylist = state.playlist.map { ep ->
                if (ep.seasonNumber == season && ep.episodeNumber == episode) update(ep) else ep
            }
            setPlayerState(state.copy(playlist = updatedPlaylist))
        }
    }

    override suspend fun executeServerAction(action: ServerAction): ActionResult {
        if (action in viewState.value.loadingActions) {
            return ActionResult.NoOp
        }

        when (action) {
            is SaveEpisodeProgressCommand -> updatePlayingEpisode(action.season, action.episode) {
                it.copy(
                    watchedProgressSeconds = action.progressSeconds,
                    isWatched = if (action.isWatched) true else it.isWatched
                )
            }

            is ToggleEpisodeWatchedCommand -> {
                updatePlayingEpisode(action.seasonNumber, action.episodeNumber) {
                    it.copy(isWatched = action.isWatched)
                }
                updateViewState { current ->
                    val seasonsData = current.tvSeasons?.state?.data ?: return@updateViewState current
                    val updatedSeasonContents = seasonsData.seasonContents.mapValues { (seasonNum, content) ->
                        if (seasonNum == action.seasonNumber) {
                            content.copy(
                                episodes = content.episodes?.map { ep ->
                                    if (ep.episodeNumber == action.episodeNumber) {
                                        ep.copy(
                                            isWatched = action.isWatched,
                                            toggleWatchedAction = ToggleEpisodeWatchedCommand(
                                                key = action.key,
                                                seasonNumber = action.seasonNumber,
                                                episodeNumber = action.episodeNumber,
                                                isWatched = !action.isWatched
                                            )
                                        )
                                    } else ep
                                }
                            )
                        } else content
                    }
                    val updatedSeasons = seasonsData.seasons.map { s ->
                        if (s.seasonNumber == action.seasonNumber) {
                            val eps = updatedSeasonContents[s.seasonNumber]?.episodes.orEmpty()
                            val watchedCount = eps.count { it.isWatched }
                            val isFullyWatched = watchedCount > 0 && watchedCount >= s.episodeCount
                            s.copy(
                                isFullyWatched = isFullyWatched,
                                isWatching = watchedCount > 0 && watchedCount < s.episodeCount,
                                markWatchedAction = MarkSeasonWatchedCommand(action.key, s.seasonNumber, !isFullyWatched)
                            )
                        } else s
                    }
                    current.copy(
                        tvSeasons = current.tvSeasons.copy(
                            state = current.tvSeasons.state.copy(
                                data = seasonsData.copy(
                                    seasons = updatedSeasons,
                                    seasonContents = updatedSeasonContents
                                )
                            )
                        )
                    )
                }
            }

            is RateEpisodeCommand -> {
                updatePlayingEpisode(action.seasonNumber, action.episodeNumber) {
                    it.copy(userRating = action.rating)
                }
                updateViewState { current ->
                    val seasonsData = current.tvSeasons?.state?.data ?: return@updateViewState current
                    val updatedSeasonContents = seasonsData.seasonContents.mapValues { (seasonNum, content) ->
                        if (seasonNum == action.seasonNumber) {
                            content.copy(
                                episodes = content.episodes?.map { ep ->
                                    if (ep.episodeNumber == action.episodeNumber) ep.copy(userRating = action.rating) else ep
                                }
                            )
                        } else content
                    }
                    current.copy(
                        tvSeasons = current.tvSeasons.copy(
                            state = current.tvSeasons.state.copy(
                                data = seasonsData.copy(seasonContents = updatedSeasonContents)
                            )
                        )
                    )
                }
            }

            is ToggleCollectionCommand -> {
                updateViewState { current ->
                    val currentGroup = current.collectionButtons?.state?.data ?: return@updateViewState current
                    val updatedButtons = currentGroup.buttons.map { btn ->
                        if (btn.customLists == null && btn.createListActionTemplate == null) {
                            btn.copy(
                                icon = if (action.inCollection) IconType.HEART_FILLED else IconType.HEART,
                                action = ToggleCollectionCommand(action.key, !action.inCollection)
                            )
                        } else btn
                    }
                    current.copy(
                        collectionButtons = current.collectionButtons.copy(
                            state = current.collectionButtons.state.copy(
                                data = currentGroup.copy(buttons = updatedButtons)
                            )
                        )
                    )
                }
            }

            is ToggleCustomListCommand -> {
                updateViewState { current ->
                    val currentGroup = current.collectionButtons?.state?.data ?: return@updateViewState current
                    val updatedButtons = currentGroup.buttons.map { btn ->
                        val lists = btn.customLists
                        if (lists != null) {
                            val updatedLists = lists.map { list ->
                                if (list.id == action.listId) {
                                    list.copy(isAdded = !list.isAdded)
                                } else list
                            }
                            btn.copy(customLists = updatedLists)
                        } else btn
                    }
                    current.copy(
                        collectionButtons = current.collectionButtons.copy(
                            state = current.collectionButtons.state.copy(
                                data = currentGroup.copy(buttons = updatedButtons)
                            )
                        )
                    )
                }
            }

            is SetStatusCommand -> {
                updateViewState { current ->
                    val userActions = current.userActions?.state?.data ?: return@updateViewState current
                    current.copy(
                        userActions = current.userActions.copy(
                            state = current.userActions.state.copy(
                                data = userActions.copy(currentStatus = action.status)
                            )
                        )
                    )
                }
            }

            is SetRatingCommand -> {
                updateViewState { current ->
                    val userActions = current.userActions?.state?.data ?: return@updateViewState current
                    current.copy(
                        userActions = current.userActions.copy(
                            state = current.userActions.state.copy(
                                data = userActions.copy(currentRating = action.rating)
                            )
                        )
                    )
                }
            }

            is MarkSeasonWatchedCommand -> {
                updateViewState { current ->
                    val seasonsData = current.tvSeasons?.state?.data ?: return@updateViewState current
                    val updatedSeasons = seasonsData.seasons.map { s ->
                        if (s.seasonNumber == action.seasonNumber) {
                            s.copy(
                                isFullyWatched = action.isWatched,
                                isWatching = false,
                                markWatchedAction = MarkSeasonWatchedCommand(action.key, action.seasonNumber, !action.isWatched)
                            )
                        } else s
                    }
                    val updatedSeasonContents = seasonsData.seasonContents.mapValues { (seasonNum, content) ->
                        if (seasonNum == action.seasonNumber) {
                            content.copy(
                                episodes = content.episodes?.map { ep ->
                                    ep.copy(
                                        isWatched = action.isWatched,
                                        toggleWatchedAction = ToggleEpisodeWatchedCommand(
                                            key = action.key,
                                            seasonNumber = seasonNum,
                                            episodeNumber = ep.episodeNumber,
                                            isWatched = !action.isWatched
                                        )
                                    )
                                }
                            )
                        } else content
                    }
                    current.copy(
                        tvSeasons = current.tvSeasons.copy(
                            state = current.tvSeasons.state.copy(
                                data = seasonsData.copy(
                                    seasons = updatedSeasons,
                                    seasonContents = updatedSeasonContents
                                )
                            )
                        )
                    )
                }
            }

            is SelectSeasonCommand -> {
                updateViewState { current ->
                    val seasonsData = current.tvSeasons?.state?.data ?: return@updateViewState current
                    current.copy(
                        tvSeasons = current.tvSeasons.copy(
                            state = current.tvSeasons.state.copy(
                                data = seasonsData.copy(selectedSeasonNumber = action.seasonNumber)
                            )
                        )
                    )
                }
            }
        }

        val previousState = viewState.value
        updateViewState { it.copy(loadingActions = it.loadingActions + action) }
        return try {
            val result = executeServerAction.invoke(action)
            if (result is ActionResult.Error) {
                updateViewState { previousState.copy(loadingActions = it.loadingActions) }
            }
            result
        } catch (e: Exception) {
            updateViewState { previousState.copy(loadingActions = it.loadingActions) }
            if (e is kotlinx.coroutines.CancellationException) throw e
            ActionResult.Error(500, e.message ?: "Action failed")
        } finally {
            updateViewState { it.copy(loadingActions = it.loadingActions - action) }
        }
    }

    fun toggleSources(expanded: Boolean) {
        updateViewState { it.copy(isSourcesExpanded = expanded) }
    }

    fun closePlayer() {
        setPlayerState(DetailsViewState.PlayerState.Idle)
    }

    fun openSourcesSheet(forceRefresh: Boolean = false) {
        setPlayerState(DetailsViewState.PlayerState.Idle)
        toggleSources(true)
        viewModelScope.launch {
            searchMediaSources(mediaKey, forceRefresh)
        }
    }

    fun selectSource(
        providerId: String,
        sourceId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                when (val result = selectMediaSource(mediaKey, providerId, sourceId, seasonNumber, episodeNumber)) {
                    is SourceSelectionResult.Ready -> {
                        toggleSources(false)
                        setPlayerState(
                            DetailsViewState.PlayerState.Preparing(
                                title = viewState.value.header?.state?.data?.title ?: "",
                                hasEpisodes = mediaKey.type == EntityType.TV,
                                targetSeason = result.targetSeason ?: seasonNumber,
                                targetEpisode = result.targetEpisode ?: episodeNumber,
                                playlist = emptyList()
                            )
                        )
                    }
                    is SourceSelectionResult.RequiresManualMapping -> {
                        updateViewState {
                            it.copy(
                                torrentInspector = SduiSlot(
                                    nodeId = "torrserver-plugin",
                                    state = SlotUiState(
                                        data = SlotData.TorrentInspector(
                                            torrentHash = result.torrentHash,
                                            torrentTitle = result.torrentTitle,
                                            files = result.files
                                        )
                                    )
                                )
                            )
                        }
                    }

                    is SourceSelectionResult.Error -> {
                    }
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }


    fun setPlayerState(state: DetailsViewState.PlayerState) {
        updateViewState { it.copy(playerState = state, isSourcesExpanded = false) }
    }

    private fun handlePlayVideo(action: ActionPlayVideo) {
        val serverUrl = tokenStorage.cachedServerUrl?.takeIf { it.isNotBlank() }
            ?: org.ensodai.avalonmediacard.data.platformServerUrl

        val resolveUrl = { url: String ->
            StreamUrlResolver.resolveAbsoluteUrl(url, serverUrl)
        }

        setPlayerState(
            DetailsViewState.PlayerState.Playing(
                streamUrl = resolveUrl(action.url),
                title = action.title,
                streamId = action.streamId,
                duration = action.durationSeconds,
                startPositionSeconds = action.startPositionSeconds,
                playlist = action.playlist.map { it.copy(url = resolveUrl(it.url)) },
                audioTracks = action.audioTracks,
                subtitleTracks = action.subtitleTracks,
                audioTrackIndex = action.audioTrackIndex
            )
        )
    }

    override fun handleLocalAction(action: Action) {
        when (action) {
            is ActionPlayVideo -> handlePlayVideo(action)
            is ActionOpenSources -> {
                openSourcesSheet()
            }
            is ActionPreparePlayer -> {

                toggleSources(false)
                val canonicalTitle = viewState.value.header?.state?.data?.title?.takeIf { it.isNotBlank() } ?: action.title
                setPlayerState(
                    DetailsViewState.PlayerState.Preparing(
                        title = canonicalTitle,
                        hasEpisodes = action.key.type == EntityType.TV,
                        playlist = action.playlist,
                        targetSeason = action.targetSeason,
                        targetEpisode = action.targetEpisode
                    )
                )
            }

            else -> super.handleLocalAction(action)
        }
    }
}
