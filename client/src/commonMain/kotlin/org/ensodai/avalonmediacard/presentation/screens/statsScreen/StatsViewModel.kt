package org.ensodai.avalonmediacard.presentation.screens.statsScreen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.domain.repository.StatsRepository
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.action.StatsActions
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.viewState.StatsViewState
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class StatsViewModel(
    private val statsRepository: StatsRepository
) : BaseViewModel<StatsViewState, StatsActions>(StatsViewState()) {

    private val logger = AppLogging.logger("StatsViewModel")

    override val actions: StatsActions = StatsActions(
        onRefresh = { loadStats() },
        onPersonClicked = { personId, name ->
            logger.i { "Person clicked: $personId ($name)" }
        },
        onShareArchetype = {
            logger.i { "Share archetype clicked" }
        }
    )

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            updateViewState { it.copy(isLoading = true, errorMessage = null) }
            try {
                val stats = statsRepository.getUserStats()
                updateViewState {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to load user stats" }
                updateViewState {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }
}
