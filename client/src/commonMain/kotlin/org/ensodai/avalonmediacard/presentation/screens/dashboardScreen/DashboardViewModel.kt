package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.DashboardViewState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.FeedItem
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(
    @InjectedParam private val screen: Screen,
    private val streamDashboardSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val manifestRepository: GlobalManifestRepository,
    private val appSettingsStorage: AppSettingsStorage
) : SduiViewModel<DashboardViewState>(
    initialState = DashboardViewState()
) {
    private val logger = AppLogging.logger("DashboardViewModel")
    private var streamJob: Job? = null

    init {
        val manifest = manifestRepository.getScreenManifest("Dashboard")
        val layout = manifest?.layout ?: emptyList()

        val initialItems = layout.map { node ->
            when (node.slotId) {
                SlotId.HeroBanner -> FeedItem.HeroBanner(node.nodeId, SlotUiState(isLoading = true))
                SlotId.CarouselBackdrops -> FeedItem.Backdrops(node.nodeId, SlotUiState(isLoading = true))
                SlotId.Exploration -> FeedItem.Exploration(node.nodeId, SlotUiState(isLoading = true))
                else -> FeedItem.Carousel(node.nodeId, SlotUiState(isLoading = true))
            }
        }

        updateViewState {
            it.copy(feedItems = initialItems)
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
                                    if (item.nodeId == update.nodeId) { // Пока используем pluginId в FeedItem, но мэтчим с nodeId
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

/**
 * Сбрасывает состояние FeedItem в Loading без сохранения старых данных.
 */
fun FeedItem.toLoading(): FeedItem {
    return when (this) {
        is FeedItem.HeroBanner -> copy(state = SlotUiState(isLoading = true))
        is FeedItem.Carousel -> copy(state = SlotUiState(isLoading = true))
        is FeedItem.Backdrops -> copy(state = SlotUiState(isLoading = true))
        is FeedItem.Exploration -> copy(state = SlotUiState(isLoading = true))
    }
}

/**
 * Обновляет state внутри FeedItem, сохраняя его тип.
 * Старые данные сохраняются при Loading (чтобы не моргал контент).
 */
fun FeedItem.withUpdate(slotState: SlotState): FeedItem {
    return when (this) {
        is FeedItem.HeroBanner -> copy(state = slotState.toUiState(state.data))
        is FeedItem.Carousel -> copy(state = slotState.toUiState(state.data))
        is FeedItem.Backdrops -> copy(state = slotState.toUiState(state.data))
        is FeedItem.Exploration -> copy(state = slotState.toUiState(state.data))
    }
}

/**
 * Конвертирует серверный SlotState в клиентский SlotUiState.
 * При Loading сохраняет предыдущие данные (oldData).
 */
@Suppress("UNCHECKED_CAST")
private fun <T> SlotState.toUiState(oldData: T?): SlotUiState<T> {
    return when (this) {
        is SlotState.Loading -> SlotUiState(isLoading = true, data = oldData)
        is SlotState.Content -> SlotUiState(data = this.data as? T ?: oldData)
        is SlotState.Error -> SlotUiState(error = this.message, retryAction = this.retryAction, data = oldData)
        is SlotState.Empty -> SlotUiState()
    }
}
