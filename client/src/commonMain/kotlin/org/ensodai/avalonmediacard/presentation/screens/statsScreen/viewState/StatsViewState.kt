package org.ensodai.avalonmediacard.presentation.screens.statsScreen.viewState

import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState

data class StatsViewState(
    val isLoading: Boolean = true,
    val stats: UserStatsDto? = null,
    val errorMessage: String? = null
) : BaseViewState()
