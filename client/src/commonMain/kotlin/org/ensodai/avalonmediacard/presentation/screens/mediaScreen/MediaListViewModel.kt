package org.ensodai.avalonmediacard.presentation.screens.mediaScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.StreamScreenSlotsUseCase
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewModel
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.core.extractSlots
import org.ensodai.avalonmediacard.presentation.screens.mediaScreen.viewState.MediaListViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MediaListViewModel(
    @InjectedParam private val screen: Screen.MediaList,
    private val streamScreenSlots: StreamScreenSlotsUseCase,
    private val executeServerAction: ExecuteServerActionUseCase,
    private val appSettingsStorage: AppSettingsStorage
) : SduiViewModel<MediaListViewState>(MediaListViewState()) {

    private var streamJob: Job? = null

    init {
        updateViewState {
            it.copy(
                grids = listOf(
                    SduiSlot("mirage", SlotUiState(isLoading = true))
                )
            )
        }
        loadSlots()

        viewModelScope.launch {
            appSettingsStorage.settingsVersion.drop(1).collect {
                updateViewState {
                    it.copy(
                        grids = listOf(
                            SduiSlot("mirage", SlotUiState(isLoading = true))
                        )
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

                streamScreenSlots(screen).collect { event ->
                    when (event) {
                        is ScreenStreamEvent.Layout -> {}
                        is ScreenStreamEvent.Update -> {
                            val update = event.update
                            val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                            slotMap[update.nodeId] = update

                            updateViewState {
                                it.copy(
                                    grids = if (currentSlotsMap[SlotId.MediaList] == null) it.grids else currentSlotsMap.extractSlots<SlotData.Grid>(
                                        SlotId.MediaList,
                                        oldSlots = it.grids
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
