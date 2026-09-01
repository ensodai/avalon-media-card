package org.ensodai.avalonmediacard.presentation.screens.admin.action

import androidx.lifecycle.viewModelScope
import avalonmediacard.client.generated.resources.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.admin.UpdateGlobalIntegrationSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateJackettSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateProwlarrSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateTmdbSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateTorrServerSettingsRequest
import org.jetbrains.compose.resources.getString
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.*
import org.ensodai.avalonmediacard.presentation.screens.admin.AdminViewModel
import org.ensodai.avalonmediacard.presentation.screens.admin.model.AdminTab

fun AdminViewModel.onTabSelected(tab: AdminTab) {
    updateViewState { it.copy(selectedTab = tab, error = null, successMessage = null) }
}

fun AdminViewModel.onUsernameChanged(username: String) {
    updateViewState { it.copy(usernameInput = username, error = null, successMessage = null) }
}

fun AdminViewModel.onPasswordChanged(password: String) {
    updateViewState { it.copy(passwordInput = password, error = null, successMessage = null) }
}

fun AdminViewModel.onCreateUserClicked(addUserUseCase: AddUserUseCase) {
    val state = viewState.value
    if (state.usernameInput.isBlank() || state.passwordInput.isBlank()) {
        viewModelScope.launch {
            val msg = getString(Res.string.admin_msg_fill_all_fields)
            updateViewState { it.copy(error = msg) }
        }
        return
    }
    updateViewState { it.copy(isLoading = true, error = null, successMessage = null) }

    viewModelScope.launch {
        addUserUseCase(
            username = state.usernameInput,
            passwordRaw = state.passwordInput,
            role = UserRole.USER
        ).fold(
            onSuccess = {
                val msg = getString(Res.string.admin_msg_user_created, state.usernameInput)
                updateViewState {
                    it.copy(
                        isLoading = false,
                        successMessage = msg,
                        usernameInput = "",
                        passwordInput = ""
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_unknown_error)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isLoading = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.clearMessages() {
    updateViewState { it.copy(error = null, successMessage = null) }
}

fun AdminViewModel.loadUsers(getUsersUseCase: GetUsersUseCase) {
    updateViewState { it.copy(isUsersLoading = true, usersError = null) }
    viewModelScope.launch {
        getUsersUseCase().fold(
            onSuccess = { users ->
                updateViewState {
                    it.copy(
                        isUsersLoading = false,
                        users = users
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_load_users)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isUsersLoading = false,
                        usersError = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.onUserStatusChange(
    userId: String,
    status: UserStatus,
    updateUserStatusUseCase: UpdateUserStatusUseCase,
    getUsersUseCase: GetUsersUseCase
) {
    viewModelScope.launch {
        updateUserStatusUseCase(userId, status).fold(
            onSuccess = {
                loadUsers(getUsersUseCase)
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_update_status)
                val msg = exception.message ?: fallback
                updateViewState { it.copy(usersError = msg) }
            }
        )
    }
}

fun AdminViewModel.onUserRoleChange(
    userId: String,
    role: UserRole,
    updateUserRoleUseCase: UpdateUserRoleUseCase,
    getUsersUseCase: GetUsersUseCase
) {
    viewModelScope.launch {
        updateUserRoleUseCase(userId, role).fold(
            onSuccess = {
                loadUsers(getUsersUseCase)
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_change_role)
                val msg = exception.message ?: fallback
                updateViewState { it.copy(usersError = msg) }
            }
        )
    }
}

fun AdminViewModel.onDeleteUser(
    userId: String,
    deleteUserUseCase: DeleteUserUseCase,
    getUsersUseCase: GetUsersUseCase
) {
    viewModelScope.launch {
        deleteUserUseCase(userId).fold(
            onSuccess = {
                loadUsers(getUsersUseCase)
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_delete_user)
                val msg = exception.message ?: fallback
                updateViewState { it.copy(usersError = msg) }
            }
        )
    }
}

fun AdminViewModel.onResetUserPassword(
    userId: String,
    newPasswordRaw: String,
    resetUserPasswordUseCase: ResetUserPasswordUseCase
) {
    viewModelScope.launch {
        resetUserPasswordUseCase(userId, newPasswordRaw).fold(
            onSuccess = {
                val msg = getString(Res.string.admin_msg_password_reset_success, newPasswordRaw)
                updateViewState { 
                    it.copy(successMessage = msg, error = null) 
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_reset_password)
                val msg = exception.message ?: fallback
                updateViewState { 
                    it.copy(usersError = msg) 
                }
            }
        )
    }
}

fun AdminViewModel.onTmdbTokenChanged(token: String) {
    updateViewState { it.copy(tmdbReadTokenInput = token, tmdbTestResult = null, tmdbTestSuccess = null) }
}

fun AdminViewModel.onTmdbShareChanged(share: Boolean) {
    updateViewState { it.copy(tmdbShareWithUsers = share) }
}

fun AdminViewModel.onTorrServerHostChanged(host: String) {
    updateViewState { it.copy(torrServerHostInput = host, torrServerTestResult = null, torrServerTestSuccess = null) }
}

fun AdminViewModel.onTorrServerLoginChanged(login: String) {
    updateViewState { it.copy(torrServerLoginInput = login, torrServerTestResult = null, torrServerTestSuccess = null) }
}

fun AdminViewModel.onTorrServerPasswordChanged(password: String) {
    updateViewState { it.copy(torrServerPasswordInput = password, torrServerTestResult = null, torrServerTestSuccess = null) }
}

fun AdminViewModel.onTorrServerShareChanged(share: Boolean) {
    updateViewState { it.copy(torrServerShareWithUsers = share) }
}

fun AdminViewModel.onTorrServerUseGstChanged(useGst: Boolean) {
    updateViewState { it.copy(torrServerUseGst = useGst) }
}

fun AdminViewModel.onProwlarrUrlChanged(url: String) {
    updateViewState { it.copy(prowlarrUrlInput = url, prowlarrTestResult = null, prowlarrTestSuccess = null) }
}

fun AdminViewModel.onProwlarrApiKeyChanged(key: String) {
    updateViewState { it.copy(prowlarrApiKeyInput = key, prowlarrTestResult = null, prowlarrTestSuccess = null) }
}

fun AdminViewModel.onProwlarrShareChanged(share: Boolean) {
    updateViewState { it.copy(prowlarrShareWithUsers = share) }
}

fun AdminViewModel.onJackettUrlChanged(url: String) {
    updateViewState { it.copy(jackettUrlInput = url, jackettTestResult = null, jackettTestSuccess = null) }
}

fun AdminViewModel.onJackettApiKeyChanged(key: String) {
    updateViewState { it.copy(jackettApiKeyInput = key, jackettTestResult = null, jackettTestSuccess = null) }
}

fun AdminViewModel.onJackettShareChanged(share: Boolean) {
    updateViewState { it.copy(jackettShareWithUsers = share) }
}

fun AdminViewModel.loadGlobalIntegrations(
    getGlobalIntegrationSettingsUseCase: GetGlobalIntegrationSettingsUseCase
) {
    updateViewState { it.copy(isIntegrationsLoading = true, error = null) }
    viewModelScope.launch {
        getGlobalIntegrationSettingsUseCase().fold(
            onSuccess = { settings ->
                updateViewState {
                    it.copy(
                        isIntegrationsLoading = false,
                        tmdbReadTokenInput = settings.tmdbReadToken ?: "",
                        tmdbShareWithUsers = settings.tmdbShareWithUsers,
                        torrServerHostInput = settings.torrServerHost ?: "",
                        torrServerLoginInput = settings.torrServerLogin ?: "",
                        torrServerPasswordInput = settings.torrServerPassword ?: "",
                        torrServerShareWithUsers = settings.torrServerShareWithUsers,
                        torrServerUseGst = settings.torrServerUseGst,
                        prowlarrUrlInput = settings.prowlarrUrl ?: "",
                        prowlarrApiKeyInput = settings.prowlarrApiKey ?: "",
                        prowlarrShareWithUsers = settings.prowlarrShareWithUsers,
                        jackettUrlInput = settings.jackettUrl ?: "",
                        jackettApiKeyInput = settings.jackettApiKey ?: "",
                        jackettShareWithUsers = settings.jackettShareWithUsers
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_load_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isIntegrationsLoading = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.saveGlobalIntegrations(
    updateGlobalIntegrationSettingsUseCase: UpdateGlobalIntegrationSettingsUseCase,
    manifestRepository: GlobalManifestRepository,
    appSettingsStorage: AppSettingsStorage
) {
    val state = viewState.value
    updateViewState { it.copy(isIntegrationsLoading = true, error = null, successMessage = null) }
    viewModelScope.launch {
        updateGlobalIntegrationSettingsUseCase(
            UpdateGlobalIntegrationSettingsRequest(
                tmdbReadToken = state.tmdbReadTokenInput,
                tmdbShareWithUsers = state.tmdbShareWithUsers,
                torrServerHost = state.torrServerHostInput,
                torrServerLogin = state.torrServerLoginInput,
                torrServerPassword = state.torrServerPasswordInput,
                torrServerShareWithUsers = state.torrServerShareWithUsers,
                torrServerUseGst = state.torrServerUseGst,
                prowlarrUrl = state.prowlarrUrlInput,
                prowlarrApiKey = state.prowlarrApiKeyInput,
                prowlarrShareWithUsers = state.prowlarrShareWithUsers,
                jackettUrl = state.jackettUrlInput,
                jackettApiKey = state.jackettApiKeyInput,
                jackettShareWithUsers = state.jackettShareWithUsers
            )
        ).fold(
            onSuccess = {
                manifestRepository.refreshManifest()
                appSettingsStorage.notifySettingsChanged()
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState {
                    it.copy(
                        isIntegrationsLoading = false,
                        successMessage = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_save_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isIntegrationsLoading = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.saveTmdbSettings(
    updateTmdbSettingsUseCase: UpdateTmdbSettingsUseCase,
    manifestRepository: GlobalManifestRepository,
    appSettingsStorage: AppSettingsStorage
) {
    val state = viewState.value
    updateViewState { it.copy(isTmdbSaving = true, error = null, successMessage = null) }
    viewModelScope.launch {
        updateTmdbSettingsUseCase(
            UpdateTmdbSettingsRequest(
                token = state.tmdbReadTokenInput,
                shareWithUsers = state.tmdbShareWithUsers
            )
        ).fold(
            onSuccess = {
                manifestRepository.refreshManifest()
                appSettingsStorage.notifySettingsChanged()
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState {
                    it.copy(
                        isTmdbSaving = false,
                        successMessage = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_save_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isTmdbSaving = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.saveTorrServerSettings(
    updateTorrServerSettingsUseCase: UpdateTorrServerSettingsUseCase,
    appSettingsStorage: AppSettingsStorage
) {
    val state = viewState.value
    updateViewState { it.copy(isTorrServerSaving = true, error = null, successMessage = null) }
    viewModelScope.launch {
        updateTorrServerSettingsUseCase(
            UpdateTorrServerSettingsRequest(
                host = state.torrServerHostInput,
                login = state.torrServerLoginInput,
                password = state.torrServerPasswordInput,
                shareWithUsers = state.torrServerShareWithUsers,
                useGst = state.torrServerUseGst
            )
        ).fold(
            onSuccess = {
                appSettingsStorage.notifySettingsChanged()
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState {
                    it.copy(
                        isTorrServerSaving = false,
                        successMessage = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_save_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isTorrServerSaving = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.saveProwlarrSettings(
    updateProwlarrSettingsUseCase: UpdateProwlarrSettingsUseCase,
    appSettingsStorage: AppSettingsStorage
) {
    val state = viewState.value
    updateViewState { it.copy(isProwlarrSaving = true, error = null, successMessage = null) }
    viewModelScope.launch {
        updateProwlarrSettingsUseCase(
            UpdateProwlarrSettingsRequest(
                url = state.prowlarrUrlInput,
                apiKey = state.prowlarrApiKeyInput,
                shareWithUsers = state.prowlarrShareWithUsers
            )
        ).fold(
            onSuccess = {
                appSettingsStorage.notifySettingsChanged()
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState {
                    it.copy(
                        isProwlarrSaving = false,
                        successMessage = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_save_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isProwlarrSaving = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.saveJackettSettings(
    updateJackettSettingsUseCase: UpdateJackettSettingsUseCase,
    appSettingsStorage: AppSettingsStorage
) {
    val state = viewState.value
    updateViewState { it.copy(isJackettSaving = true, error = null, successMessage = null) }
    viewModelScope.launch {
        updateJackettSettingsUseCase(
            UpdateJackettSettingsRequest(
                url = state.jackettUrlInput,
                apiKey = state.jackettApiKeyInput,
                shareWithUsers = state.jackettShareWithUsers
            )
        ).fold(
            onSuccess = {
                appSettingsStorage.notifySettingsChanged()
                val msg = getString(Res.string.admin_msg_settings_saved)
                updateViewState {
                    it.copy(
                        isJackettSaving = false,
                        successMessage = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_error_save_settings)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isJackettSaving = false,
                        error = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.testTmdbConnection(
    testTmdbConnectionUseCase: TestTmdbConnectionUseCase
) {
    val state = viewState.value
    if (state.tmdbReadTokenInput.isBlank()) {
        viewModelScope.launch {
            val msg = getString(Res.string.admin_msg_specify_tmdb_token)
            updateViewState {
                it.copy(
                    isTmdbTesting = false,
                    tmdbTestSuccess = false,
                    tmdbTestResult = msg
                )
            }
        }
        return
    }

    updateViewState { it.copy(isTmdbTesting = true, tmdbTestResult = null, tmdbTestSuccess = null) }
    viewModelScope.launch {
        testTmdbConnectionUseCase(state.tmdbReadTokenInput).fold(
            onSuccess = { response ->
                val successMsg = getString(Res.string.admin_msg_tmdb_success)
                val errorFallback = getString(Res.string.admin_msg_tmdb_token_error)
                val msg = if (response.success) successMsg else (response.error ?: errorFallback)
                updateViewState {
                    it.copy(
                        isTmdbTesting = false,
                        tmdbTestSuccess = response.success,
                        tmdbTestResult = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_tmdb_test_failed)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isTmdbTesting = false,
                        tmdbTestSuccess = false,
                        tmdbTestResult = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.testTorrServerConnection(
    testTorrServerConnectionUseCase: TestTorrServerConnectionUseCase
) {
    val state = viewState.value
    if (state.torrServerHostInput.isBlank()) {
        viewModelScope.launch {
            val msg = getString(Res.string.admin_msg_specify_torrserver_url)
            updateViewState {
                it.copy(
                    isTorrServerTesting = false,
                    torrServerTestSuccess = false,
                    torrServerTestResult = msg
                )
            }
        }
        return
    }

    updateViewState { it.copy(isTorrServerTesting = true, torrServerTestResult = null, torrServerTestSuccess = null) }
    viewModelScope.launch {
        testTorrServerConnectionUseCase(
            host = state.torrServerHostInput,
            login = state.torrServerLoginInput.takeIf { it.isNotBlank() },
            password = state.torrServerPasswordInput.takeIf { it.isNotBlank() }
        ).fold(
            onSuccess = { response ->
                val successMsg = getString(Res.string.admin_msg_torrserver_success)
                val errorFallback = getString(Res.string.admin_msg_connection_error)
                val msg = if (response.success) successMsg else (response.error ?: errorFallback)
                updateViewState {
                    it.copy(
                        isTorrServerTesting = false,
                        torrServerTestSuccess = response.success,
                        torrServerTestResult = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_torrserver_test_failed)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isTorrServerTesting = false,
                        torrServerTestSuccess = false,
                        torrServerTestResult = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.testProwlarrConnection(
    testProwlarrConnectionUseCase: TestProwlarrConnectionUseCase
) {
    val state = viewState.value
    if (state.prowlarrUrlInput.isBlank()) {
        viewModelScope.launch {
            val msg = getString(Res.string.admin_msg_specify_prowlarr_url)
            updateViewState {
                it.copy(
                    isProwlarrTesting = false,
                    prowlarrTestSuccess = false,
                    prowlarrTestResult = msg
                )
            }
        }
        return
    }

    updateViewState { it.copy(isProwlarrTesting = true, prowlarrTestResult = null, prowlarrTestSuccess = null) }
    viewModelScope.launch {
        testProwlarrConnectionUseCase(state.prowlarrUrlInput, state.prowlarrApiKeyInput).fold(
            onSuccess = { response ->
                val successMsg = getString(Res.string.admin_msg_prowlarr_success)
                val errorFallback = getString(Res.string.admin_msg_connection_error)
                val msg = if (response.success) successMsg else (response.error ?: errorFallback)
                updateViewState {
                    it.copy(
                        isProwlarrTesting = false,
                        prowlarrTestSuccess = response.success,
                        prowlarrTestResult = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_prowlarr_test_failed)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isProwlarrTesting = false,
                        prowlarrTestSuccess = false,
                        prowlarrTestResult = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.testJackettConnection(
    testJackettConnectionUseCase: TestJackettConnectionUseCase
) {
    val state = viewState.value
    if (state.jackettUrlInput.isBlank()) {
        viewModelScope.launch {
            val msg = getString(Res.string.admin_msg_specify_jackett_url)
            updateViewState {
                it.copy(
                    isJackettTesting = false,
                    jackettTestSuccess = false,
                    jackettTestResult = msg
                )
            }
        }
        return
    }

    updateViewState { it.copy(isJackettTesting = true, jackettTestResult = null, jackettTestSuccess = null) }
    viewModelScope.launch {
        testJackettConnectionUseCase(state.jackettUrlInput, state.jackettApiKeyInput).fold(
            onSuccess = { response ->
                val successMsg = getString(Res.string.admin_msg_jackett_success)
                val errorFallback = getString(Res.string.admin_msg_connection_error)
                val msg = if (response.success) successMsg else (response.error ?: errorFallback)
                updateViewState {
                    it.copy(
                        isJackettTesting = false,
                        jackettTestSuccess = response.success,
                        jackettTestResult = msg
                    )
                }
            },
            onFailure = { exception ->
                val fallback = getString(Res.string.admin_msg_jackett_test_failed)
                val msg = exception.message ?: fallback
                updateViewState {
                    it.copy(
                        isJackettTesting = false,
                        jackettTestSuccess = false,
                        jackettTestResult = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.loadSystemInfo(getSystemInfoUseCase: GetSystemInfoUseCase) {
    updateViewState { it.copy(isSystemLoading = true) }
    viewModelScope.launch {
        getSystemInfoUseCase().fold(
            onSuccess = { info ->
                updateViewState {
                    it.copy(
                        isSystemLoading = false,
                        systemInfo = info
                    )
                }
            },
            onFailure = { error ->
                val msg = getString(Res.string.admin_msg_error_load_system, error.message ?: "")
                updateViewState {
                    it.copy(
                        isSystemLoading = false,
                        systemActionMessage = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.clearDiscoverCache(
    clearDiscoverCacheUseCase: ClearDiscoverCacheUseCase,
    getSystemInfoUseCase: GetSystemInfoUseCase
) {
    updateViewState { it.copy(isSystemActionLoading = true, systemActionMessage = null) }
    viewModelScope.launch {
        clearDiscoverCacheUseCase().fold(
            onSuccess = { res ->
                val defaultMsg = getString(Res.string.admin_msg_cache_tmdb_flushed)
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = res.error ?: defaultMsg
                    )
                }
                loadSystemInfo(getSystemInfoUseCase)
            },
            onFailure = { err ->
                val msg = getString(Res.string.admin_msg_error_flush_cache, err.message ?: "")
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.clearFeedCache(
    clearFeedCacheUseCase: ClearFeedCacheUseCase,
    getSystemInfoUseCase: GetSystemInfoUseCase
) {
    updateViewState { it.copy(isSystemActionLoading = true, systemActionMessage = null) }
    viewModelScope.launch {
        clearFeedCacheUseCase().fold(
            onSuccess = { res ->
                val defaultMsg = getString(Res.string.admin_msg_cache_feed_flushed)
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = res.error ?: defaultMsg
                    )
                }
                loadSystemInfo(getSystemInfoUseCase)
            },
            onFailure = { err ->
                val msg = getString(Res.string.admin_msg_error_flush_feed, err.message ?: "")
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.clearMediaCache(
    clearMediaCacheUseCase: ClearMediaCacheUseCase,
    getSystemInfoUseCase: GetSystemInfoUseCase
) {
    updateViewState { it.copy(isSystemActionLoading = true, systemActionMessage = null) }
    viewModelScope.launch {
        clearMediaCacheUseCase().fold(
            onSuccess = { res ->
                val defaultMsg = getString(Res.string.admin_msg_cache_media_flushed)
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = res.error ?: defaultMsg
                    )
                }
                loadSystemInfo(getSystemInfoUseCase)
            },
            onFailure = { err ->
                val msg = getString(Res.string.admin_msg_error_flush_media, err.message ?: "")
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = msg
                    )
                }
            }
        )
    }
}

fun AdminViewModel.reloadPlugins(
    reloadPluginsUseCase: ReloadPluginsUseCase,
    getSystemInfoUseCase: GetSystemInfoUseCase
) {
    updateViewState { it.copy(isSystemActionLoading = true, systemActionMessage = null) }
    viewModelScope.launch {
        reloadPluginsUseCase().fold(
            onSuccess = { res ->
                val defaultMsg = getString(Res.string.admin_msg_plugins_reloaded)
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = res.error ?: defaultMsg
                    )
                }
                loadSystemInfo(getSystemInfoUseCase)
            },
            onFailure = { err ->
                val msg = getString(Res.string.admin_msg_error_reload_plugins, err.message ?: "")
                updateViewState {
                    it.copy(
                        isSystemActionLoading = false,
                        systemActionMessage = msg
                    )
                }
            }
        )
    }
}
