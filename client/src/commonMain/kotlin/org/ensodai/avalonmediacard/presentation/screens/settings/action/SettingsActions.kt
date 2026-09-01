package org.ensodai.avalonmediacard.presentation.screens.settings.action

import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.data.UiModeOverride
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions

data class SettingsActions(
    val onToggleDarkMode: (Boolean) -> Unit,
    val onUiModeSelected: (UiModeOverride) -> Unit,
    val onLanguageSelected: (String) -> Unit,
    val onPosterLanguageSelected: (String?) -> Unit,
    val onTitleLanguageSelected: (String?) -> Unit,
    val onOverviewLanguageSelected: (String?) -> Unit,
    val onTmdbTokenChanged: (String?) -> Unit,
    val onSaveClicked: () -> Unit,
    val clearMessages: () -> Unit
) : BaseActions()
