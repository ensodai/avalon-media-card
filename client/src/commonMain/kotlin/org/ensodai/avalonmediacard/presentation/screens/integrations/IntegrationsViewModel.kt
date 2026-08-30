package org.ensodai.avalonmediacard.presentation.screens.integrations

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.core.extractSlots
import org.ensodai.avalonmediacard.presentation.screens.integrations.viewState.IntegrationsViewState
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class IntegrationsViewModel(
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase
) : SduiViewModel<IntegrationsViewState>(IntegrationsViewState()) {

    init {
        updateViewState {
            it.copy(
                settingsGroups = listOf(
                    SduiSlot("mirage", SlotUiState(isLoading = true))
                )
            )
        }
        loadSlots()
    }

    private fun loadSlots() {
        viewModelScope.launch {
            try {
                val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()

                streamScreenSlots(Screen.Integrations).collect { event ->
                    when (event) {
                        is ScreenStreamEvent.Layout -> {}
                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState {
                                it.copy(
                                    settingsGroups = if (currentSlotsMap[SlotId.Integrations] == null) it.settingsGroups else currentSlotsMap.extractSlots<SlotData.SettingsGroup>(
                                        SlotId.Integrations,
                                        oldSlots = it.settingsGroups
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
