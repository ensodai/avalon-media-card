package org.ensodai.avalonmediacard.presentation.screens.statsScreen.action

import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions

data class StatsActions(
    val onRefresh: () -> Unit,
    val onPersonClicked: (personId: String, name: String) -> Unit,
    val onShareArchetype: () -> Unit
) : BaseActions()
