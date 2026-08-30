package org.ensodai.avalonmediacard.presentation.screens.person

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.*
import org.ensodai.avalonmediacard.presentation.screens.person.viewState.PersonViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PersonViewModel(
    @InjectedParam private val screen: Screen.Person,
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase
) : SduiViewModel<PersonViewState>(PersonViewState()) {

    init {
        updateViewState {
            it.copy(
                header = SduiSlot("mirage", SlotUiState(isLoading = true)),
                bio = SduiSlot("mirage", SlotUiState(isLoading = true)),
                credits = listOf(SduiSlot("mirage", SlotUiState(isLoading = true)))
            )
        }
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
                                    header = if (currentSlotsMap[SlotId.PersonHeader] == null) it.header else currentSlotsMap.extractSlot<SlotData.Header>(
                                        SlotId.PersonHeader,
                                        oldSlot = it.header
                                    ),
                                    bio = if (currentSlotsMap[SlotId.PersonBio] == null) it.bio else currentSlotsMap.extractSlot<SlotData.Text>(
                                        SlotId.PersonBio,
                                        oldSlot = it.bio
                                    ),
                                    credits = if (currentSlotsMap[SlotId.PersonCredits] == null) it.credits else currentSlotsMap.extractSlots<SlotData.Carousel>(
                                        SlotId.PersonCredits,
                                        oldSlots = it.credits
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
