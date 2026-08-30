package org.ensodai.avalonmediacard.presentation.screens.search

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.extractSlot
import org.ensodai.avalonmediacard.presentation.screens.search.viewState.SearchViewState
import org.ensodai.avalonmediacard.presentation.telemetry.TelemetryTracker
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
class SearchViewModel(
    @InjectedParam private val screen: Screen.Search,
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val telemetryTracker: TelemetryTracker
) : SduiViewModel<SearchViewState>(SearchViewState()) {

    private val searchQueries = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        updateViewState {
            it.copy(
                resultsGrid = org.ensodai.avalonmediacard.presentation.core.SduiSlot(
                    "mirage",
                    org.ensodai.avalonmediacard.presentation.core.SlotUiState(isLoading = true)
                )
            )
        }
        loadSlots()
        setupSearchDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            searchQueries
                .debounce(500.milliseconds)
                .collect { query ->
                    telemetryTracker.logSearch(query)
                    executeServerAction(org.ensodai.avalonmediacard.contract.slot.SearchQueryCommand(query))
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQueries.tryEmit(query)
    }

    private fun loadSlots() {
        viewModelScope.launch {
            try {
                val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()

                streamScreenSlots(screen).collect { event ->
                    when (event) {
                        is org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent.Layout -> {}
                        is org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent.Update -> {
                            val update = event.update
                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState {
                                it.copy(
                                    resultsGrid = if (currentSlotsMap[SlotId.SearchResults] == null) it.resultsGrid else currentSlotsMap.extractSlot<SlotData.Grid>(
                                        SlotId.SearchResults,
                                        oldSlot = it.resultsGrid
                                    )
                                )
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

    override suspend fun executeServerAction(action: ServerAction): ActionResult {
        updateViewState { it.copy(loadingActions = it.loadingActions + action) }
        return try {
            executeServerAction.invoke(action)
        } finally {
            updateViewState { it.copy(loadingActions = it.loadingActions - action) }
        }
    }
}
