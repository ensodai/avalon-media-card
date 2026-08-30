package org.ensodai.avalonmediacard.presentation.screens.settings

import androidx.lifecycle.viewModelScope
import avalonmediacard.client.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.domain.useCases.core.GetUserSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.core.UpdateUserSettingsUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.settings.action.SettingsActions
import org.ensodai.avalonmediacard.presentation.screens.settings.viewState.SettingsViewState
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory

@Factory
class SettingsViewModel(
    private val appSettingsStorage: AppSettingsStorage,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase
) : BaseViewModel<SettingsViewState, SettingsActions>(
    initialState = SettingsViewState(
        uiModeOverride = appSettingsStorage.cachedUiModeOverride,
        uiLocale = appSettingsStorage.cachedLanguage
    )
) {
    init {
        viewModelScope.launch {
            appSettingsStorage.uiModeOverride.collect { mode ->
                updateViewState { it.copy(uiModeOverride = mode) }
            }
        }
        viewModelScope.launch {
            appSettingsStorage.language.collect { lang ->
                updateViewState { it.copy(uiLocale = lang) }
            }
        }
        viewModelScope.launch {
            val serverSettings = getUserSettingsUseCase()
            updateViewState {
                it.copy(
                    uiLocale = serverSettings.uiLocale,
                    posterLanguage = serverSettings.posterLanguage,
                    titleMode = serverSettings.titleMode,
                    titleLanguage = serverSettings.titleLanguage,
                    overviewLanguage = serverSettings.overviewLanguage
                )
            }
            if (serverSettings.uiLocale.isNotBlank()) {
                appSettingsStorage.saveLanguage(serverSettings.uiLocale)
            }
        }
    }

    override val actions = SettingsActions(
        onToggleDarkMode = { enabled ->
            updateViewState { it.copy(isDarkModeEnabled = enabled, error = null, successMessage = null) }
        },
        onUiModeSelected = { mode ->
            viewModelScope.launch {
                appSettingsStorage.saveUiModeOverride(mode)
            }
        },
        onLanguageSelected = { langCode ->
            updateViewState { it.copy(uiLocale = langCode) }
            viewModelScope.launch {
                appSettingsStorage.saveLanguage(langCode)
            }
            persistSettings()
        },
        onPosterLanguageSelected = { posterLang ->
            updateViewState { it.copy(posterLanguage = posterLang) }
            persistSettings()
        },
        onTitleLanguageSelected = { titleLang ->
            val mode = if (titleLang == "original") TitleDisplayMode.ORIGINAL else TitleDisplayMode.LOCALIZED
            updateViewState { it.copy(titleLanguage = titleLang, titleMode = mode) }
            persistSettings()
        },
        onOverviewLanguageSelected = { overviewLang ->
            updateViewState { it.copy(overviewLanguage = overviewLang) }
            persistSettings()
        },
        onSaveClicked = {
            persistSettings(showFeedback = true)
        },
        clearMessages = {
            updateViewState { it.copy(error = null, successMessage = null) }
        }
    )

    private fun persistSettings(showFeedback: Boolean = false) {
        val currentState = viewState.value
        viewModelScope.launch {
            if (showFeedback) {
                updateViewState { it.copy(isLoading = true, error = null, successMessage = null) }
            }
            val dto = UserSettingsDto(
                uiLocale = currentState.uiLocale,
                posterLanguage = currentState.posterLanguage,
                titleMode = currentState.titleMode,
                titleLanguage = currentState.titleLanguage,
                overviewLanguage = currentState.overviewLanguage
            )
            updateUserSettingsUseCase(dto)
            appSettingsStorage.notifySettingsChanged()
            if (showFeedback) {
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState { it.copy(isLoading = false, successMessage = msg) }
            }
        }
    }
}
