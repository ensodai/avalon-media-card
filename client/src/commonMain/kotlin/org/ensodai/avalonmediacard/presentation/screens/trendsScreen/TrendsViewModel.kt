package org.ensodai.avalonmediacard.presentation.screens.trendsScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.toLoading
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.DashboardViewState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.FeedItem
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.withUpdate
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TrendsViewModel(
    @InjectedParam private val screen: Screen,
    private val streamDashboardSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val manifestRepository: GlobalManifestRepository,
    private val appSettingsStorage: AppSettingsStorage
) : SduiViewModel<DashboardViewState>(
    initialState = DashboardViewState()
) {
    private val logger = AppLogging.logger("TrendsViewModel")
    private var streamJob: Job? = null

    init {
        updateViewState {
            it.copy(feedItems = emptyList())
        }
        loadSlots()

        viewModelScope.launch {
            appSettingsStorage.settingsVersion.drop(1).collect {
                updateViewState { state ->
                    state.copy(feedItems = state.feedItems.map { it.toLoading() })
                }
                loadSlots()
            }
        }
    }

    private fun loadSlots() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                streamDashboardSlots(screen).collect { event ->
                    when (event) {
                        is ScreenStreamEvent.Layout -> {
                            logger.d { "Layout received nodes: ${event.nodes.map { "${it.slotId}->${it.nodeId}" }}" }
                            updateViewState { state ->
                                val newItems = event.nodes.map { node ->
                                    val existing = state.feedItems.find { it.nodeId == node.nodeId }
                                    existing ?: when (node.slotId) {
                                        SlotId.HeroBanner -> FeedItem.HeroBanner(
                                            node.nodeId,
                                            SlotUiState(isLoading = true)
                                        )

                                        SlotId.CarouselBackdrops -> FeedItem.Backdrops(
                                            node.nodeId,
                                            SlotUiState(isLoading = true)
                                        )

                                        SlotId.Exploration -> FeedItem.Exploration(
                                            node.nodeId,
                                            SlotUiState(isLoading = true)
                                        )

                                        SlotId.Banner -> FeedItem.Banner(
                                            node.nodeId,
                                            SlotUiState(isLoading = true)
                                        )

                                        else -> FeedItem.Carousel(node.nodeId, SlotUiState(isLoading = true))
                                    }
                                }
                                state.copy(feedItems = newItems)
                            }
                        }

                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            logger.d { "Update received for slot=${update.slotId}, node=${update.nodeId}, stateClass=${update.state::class.simpleName}" }
                            updateViewState { state ->
                                val newItems = state.feedItems.map { item ->
                                    if (item.nodeId == update.nodeId) {
                                        item.withUpdate(update.state)
                                    } else {
                                        item
                                    }
                                }
                                state.copy(feedItems = newItems)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e(e) { "streamDashboardSlots error: ${e.message}" }
            }
        }
    }

    override suspend fun executeServerAction(action: ServerAction): ActionResult {
        updateViewState { it.copy(loadingActions = it.loadingActions + action) }
        return try {
            executeServerAction.invoke(action)
        } finally {
            updateViewState { it.copy(loadingActions = it.loadingActions - action) }
        }
    }
}
