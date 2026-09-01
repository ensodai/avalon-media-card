package org.ensodai.avalonmediacard.presentation.screens.settings.viewState

import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.data.UiModeOverride
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState

data class SettingsViewState(
    val isDarkModeEnabled: Boolean = true,
    val uiModeOverride: UiModeOverride = UiModeOverride.AUTO,
    val uiLocale: String = "auto",
    val posterLanguage: String? = null,
    val titleMode: TitleDisplayMode = TitleDisplayMode.LOCALIZED,
    val titleLanguage: String? = null,
    val overviewLanguage: String? = null,
    val tmdbReadToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
) : BaseViewState()
