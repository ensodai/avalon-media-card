package org.ensodai.avalonmediacard.presentation.screens.dynamic

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.*
import org.ensodai.avalonmediacard.presentation.screens.dynamic.viewState.DynamicViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DynamicViewModel(
    @InjectedParam private val screen: Screen.Dynamic,
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase
) : SduiViewModel<DynamicViewState>(DynamicViewState()) {

    init {
//        updateViewState {
//            it.copy(
//                grid = SduiSlot("mirage", SlotUiState(isLoading = true)),
//                description = listOf(SduiSlot("mirage", SlotUiState(isLoading = true)))
//            )
//        }
        loadSlots()
    }

    private fun loadSlots() {
        viewModelScope.launch {
            try {
                val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()

                streamScreenSlots(screen).collect { event ->
                    when (event) {
                        is ScreenStreamEvent.Layout -> {}
                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState {
                                it.copy(
                                    grid = if (currentSlotsMap[SlotId.MediaGrid] == null) it.grid else currentSlotsMap.extractSlot<SlotData.Grid>(
                                        SlotId.MediaGrid,
                                        oldSlot = it.grid
                                    ),
                                    description = if (currentSlotsMap[SlotId.Description] == null) it.description else currentSlotsMap.extractSlots<SlotData.Text>(
                                        SlotId.Description,
                                        oldSlots = it.description
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
