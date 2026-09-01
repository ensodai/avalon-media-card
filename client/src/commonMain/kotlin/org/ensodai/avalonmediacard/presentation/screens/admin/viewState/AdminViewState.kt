package org.ensodai.avalonmediacard.presentation.screens.admin.viewState

import org.ensodai.avalonmediacard.contract.admin.ServerSystemInfoDto
import org.ensodai.avalonmediacard.contract.admin.UserDto
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState
import org.ensodai.avalonmediacard.presentation.screens.admin.model.AdminTab

data class AdminViewState(
    val selectedTab: AdminTab = AdminTab.USERS,
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val users: List<UserDto> = emptyList(),
    val isUsersLoading: Boolean = false,
    val usersError: String? = null,
    val currentUserId: String? = null,
    val currentUsername: String? = null,
    
    // Integrations Tab
    val tmdbReadTokenInput: String = "",
    val tmdbShareWithUsers: Boolean = true,
    val isTmdbTesting: Boolean = false,
    val tmdbTestResult: String? = null,
    val tmdbTestSuccess: Boolean? = null,

    val torrServerHostInput: String = "",
    val torrServerLoginInput: String = "",
    val torrServerPasswordInput: String = "",
    val torrServerShareWithUsers: Boolean = false,
    val torrServerUseGst: Boolean = false,
    val isTorrServerTesting: Boolean = false,
    val torrServerTestResult: String? = null,
    val torrServerTestSuccess: Boolean? = null,

    val prowlarrUrlInput: String = "",
    val prowlarrApiKeyInput: String = "",
    val prowlarrShareWithUsers: Boolean = false,
    val isProwlarrTesting: Boolean = false,
    val prowlarrTestResult: String? = null,
    val prowlarrTestSuccess: Boolean? = null,

    val jackettUrlInput: String = "",
    val jackettApiKeyInput: String = "",
    val jackettShareWithUsers: Boolean = false,
    val isJackettTesting: Boolean = false,
    val jackettTestResult: String? = null,
    val jackettTestSuccess: Boolean? = null,

    val isIntegrationsLoading: Boolean = false,
    val isTmdbSaving: Boolean = false,
    val isTorrServerSaving: Boolean = false,
    val isProwlarrSaving: Boolean = false,
    val isJackettSaving: Boolean = false,

    // System Tab
    val systemInfo: ServerSystemInfoDto? = null,
    val isSystemLoading: Boolean = false,
    val systemActionMessage: String? = null,
    val isSystemActionLoading: Boolean = false
) : BaseViewState()
