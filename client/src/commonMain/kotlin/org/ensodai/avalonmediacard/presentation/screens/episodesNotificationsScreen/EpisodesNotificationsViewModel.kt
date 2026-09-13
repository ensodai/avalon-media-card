package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.MarkNotificationsReadCommand
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SetStatusCommand
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate
import org.ensodai.avalonmediacard.contract.slot.ToggleEpisodeWatchedCommand
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.core.extractSlot
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodesNotificationsViewState
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EpisodesNotificationsViewModel(
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val manifestRepository: GlobalManifestRepository,
    private val appSettingsStorage: AppSettingsStorage
) : SduiViewModel<EpisodesNotificationsViewState>(EpisodesNotificationsViewState()) {

    private var streamJob: Job? = null

    init {
        val screenName = Screen.EpisodesNotifications::class.simpleName ?: "EpisodesNotifications"
        val manifest = manifestRepository.getScreenManifest(screenName)
        val initialFeedNode = manifest?.layout?.firstOrNull { it.slotId == SlotId.EpisodesFeed }

        updateViewState {
            it.copy(
                feedSlot = initialFeedNode?.let { node ->
                    SduiSlot(node.nodeId, SlotUiState(isLoading = true))
                } ?: SduiSlot("", SlotUiState(isLoading = true))
            )
        }
        loadSlots()

        viewModelScope.launch {
            appSettingsStorage.settingsVersion.drop(1).collect {
                updateViewState { state ->
                    state.copy(
                        feedSlot = state.feedSlot?.copy(state = SlotUiState(isLoading = true))
                    )
                }
                loadSlots()
            }
        }
    }

    private fun loadSlots() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()

                streamScreenSlots(Screen.EpisodesNotifications).collect { event ->
                    when (event) {
                        is ScreenStreamEvent.Layout -> {
                            val feedNode = event.nodes.firstOrNull { it.slotId == SlotId.EpisodesFeed }
                            if (feedNode != null) {
                                updateViewState { state ->
                                    val currentSlot = state.feedSlot
                                    if (currentSlot == null || currentSlot.nodeId != feedNode.nodeId) {
                                        state.copy(
                                            feedSlot = SduiSlot(feedNode.nodeId, currentSlot?.state ?: SlotUiState(isLoading = true))
                                        )
                                    } else {
                                        state
                                    }
                                }
                            }
                        }
                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState { state ->
                                val slot = currentSlotsMap.extractSlot<SlotData.EpisodesFeed>(
                                    SlotId.EpisodesFeed,
                                    oldSlot = state.feedSlot
                                )
                                state.copy(
                                    feedSlot = slot ?: state.feedSlot
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    fun onFilterSelected(filter: EpisodeFilter) {
        updateViewState { it.copy(selectedFilter = filter) }
    }

    override suspend fun executeServerAction(action: ServerAction): ActionResult {
        if (action in viewState.value.loadingActions) {
            return ActionResult.NoOp
        }

        when (action) {
            is ToggleEpisodeWatchedCommand -> {
                updateViewState { current ->
                    val feedData = current.feedSlot?.state?.data ?: return@updateViewState current
                    var unreadDelta = 0
                    var recentUnreadDelta = 0
                    var unwatchedDelta = 0
                    val updatedSections = feedData.sections.map { section ->
                        val isRecentSection = section.sectionId == "today" || section.sectionId == "this_week"
                        section.copy(
                            episodes = section.episodes.map { ep ->
                                if (ep.mediaKey == action.key &&
                                    ep.seasonNumber == action.seasonNumber &&
                                    ep.episodeNumber == action.episodeNumber
                                ) {
                                    val wasUnread = ep.isNew
                                    if (action.isWatched) {
                                        if (wasUnread) {
                                            unreadDelta--
                                            if (isRecentSection) recentUnreadDelta--
                                        }
                                        if (!ep.isWatched) unwatchedDelta--
                                    } else {
                                        if (ep.isWatched) unwatchedDelta++
                                    }
                                    ep.copy(
                                        isWatched = action.isWatched,
                                        isNew = if (action.isWatched) false else ep.isNew,
                                        markWatchedAction = ToggleEpisodeWatchedCommand(
                                            key = action.key,
                                            seasonNumber = action.seasonNumber,
                                            episodeNumber = action.episodeNumber,
                                            isWatched = !action.isWatched
                                        )
                                    )
                                } else {
                                    ep
                                }
                            }
                        )
                    }
                    val newUnreadCount = (feedData.totalUnreadCount + unreadDelta).coerceAtLeast(0)
                    val newRecentUnreadCount = (feedData.recentUnreadCount + recentUnreadDelta).coerceAtLeast(0)
                    val newUnwatchedCount = (feedData.unwatchedCount + unwatchedDelta).coerceAtLeast(0)
                    current.copy(
                        feedSlot = current.feedSlot.copy(
                            state = current.feedSlot.state.copy(
                                data = feedData.copy(
                                    sections = updatedSections,
                                    totalUnreadCount = newUnreadCount,
                                    recentUnreadCount = newRecentUnreadCount,
                                    unwatchedCount = newUnwatchedCount
                                )
                            )
                        )
                    )
                }
            }

            is RateEpisodeCommand -> {
                updateViewState { current ->
                    val feedData = current.feedSlot?.state?.data ?: return@updateViewState current
                    val updatedSections = feedData.sections.map { section ->
                        section.copy(
                            episodes = section.episodes.map { ep ->
                                if (ep.mediaKey == action.key &&
                                    ep.seasonNumber == action.seasonNumber &&
                                    ep.episodeNumber == action.episodeNumber
                                ) {
                                    ep.copy(userRating = action.rating)
                                } else {
                                    ep
                                }
                            }
                        )
                    }
                    current.copy(
                        feedSlot = current.feedSlot.copy(
                            state = current.feedSlot.state.copy(
                                data = feedData.copy(sections = updatedSections)
                            )
                        )
                    )
                }
            }

            is SetStatusCommand -> {
                updateViewState { current ->
                    val feedData = current.feedSlot?.state?.data ?: return@updateViewState current
                    val isCompleted = action.status == MediaStatus.COMPLETED
                    var unreadDelta = 0
                    val updatedSections = feedData.sections.map { section ->
                        section.copy(
                            episodes = section.episodes.map { ep ->
                                if (ep.mediaKey == action.key) {
                                    val wasUnread = ep.isNew
                                    if (isCompleted && wasUnread) {
                                        unreadDelta--
                                    }
                                    ep.copy(
                                        isWatched = isCompleted,
                                        isNew = if (isCompleted) false else ep.isNew
                                    )
                                } else {
                                    ep
                                }
                            }
                        )
                    }
                    val newUnreadCount = (feedData.totalUnreadCount + unreadDelta).coerceAtLeast(0)
                    current.copy(
                        feedSlot = current.feedSlot.copy(
                            state = current.feedSlot.state.copy(
                                data = feedData.copy(
                                    sections = updatedSections,
                                    totalUnreadCount = newUnreadCount
                                )
                            )
                        )
                    )
                }
            }

            is MarkNotificationsReadCommand -> {
                updateViewState { current ->
                    val feedData = current.feedSlot?.state?.data ?: return@updateViewState current
                    val updatedSections = feedData.sections.map { section ->
                        section.copy(
                            episodes = section.episodes.map { ep ->
                                ep.copy(isNew = false)
                            }
                        )
                    }
                    current.copy(
                        feedSlot = current.feedSlot.copy(
                            state = current.feedSlot.state.copy(
                                data = feedData.copy(
                                    sections = updatedSections,
                                    totalUnreadCount = 0,
                                    recentUnreadCount = 0
                                )
                            )
                        )
                    )
                }
            }

            else -> {}
        }

        updateViewState { it.copy(loadingActions = it.loadingActions + action) }
        return try {
            executeServerAction.invoke(action)
        } finally {
            updateViewState { it.copy(loadingActions = it.loadingActions - action) }
        }
    }
}
