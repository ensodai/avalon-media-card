package org.ensodai.avalonmediacard.presentation.screens.admin

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.admin.CreateUserRequest
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.AddUserUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.ClearDiscoverCacheUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.ClearFeedCacheUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.ClearMediaCacheUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.DeleteUserUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.GetGlobalIntegrationSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.GetSystemInfoUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.GetUsersUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.ResetUserPasswordUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.TestJackettConnectionUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.TestProwlarrConnectionUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.TestTmdbConnectionUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.TestTorrServerConnectionUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateGlobalIntegrationSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateJackettSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateProwlarrSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateTmdbSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateTorrServerSettingsUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateUserRoleUseCase
import org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase.UpdateUserStatusUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.action.clearDiscoverCache
import org.ensodai.avalonmediacard.presentation.screens.admin.action.clearFeedCache
import org.ensodai.avalonmediacard.presentation.screens.admin.action.clearMediaCache
import org.ensodai.avalonmediacard.presentation.screens.admin.action.clearMessages
import org.ensodai.avalonmediacard.presentation.screens.admin.action.loadGlobalIntegrations
import org.ensodai.avalonmediacard.presentation.screens.admin.action.loadSystemInfo
import org.ensodai.avalonmediacard.presentation.screens.admin.action.loadUsers
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onCreateUserClicked
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onDeleteUser
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onJackettApiKeyChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onJackettShareChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onJackettUrlChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onPasswordChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onProwlarrApiKeyChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onProwlarrShareChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onProwlarrUrlChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onResetUserPassword
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTabSelected
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTmdbShareChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTmdbTokenChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTorrServerHostChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTorrServerLoginChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTorrServerPasswordChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTorrServerShareChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onTorrServerUseGstChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onUserRoleChange
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onUserStatusChange
import org.ensodai.avalonmediacard.presentation.screens.admin.action.onUsernameChanged
import org.ensodai.avalonmediacard.presentation.screens.admin.action.saveGlobalIntegrations
import org.ensodai.avalonmediacard.presentation.screens.admin.action.saveJackettSettings
import org.ensodai.avalonmediacard.presentation.screens.admin.action.saveProwlarrSettings
import org.ensodai.avalonmediacard.presentation.screens.admin.action.saveTmdbSettings
import org.ensodai.avalonmediacard.presentation.screens.admin.action.saveTorrServerSettings
import org.ensodai.avalonmediacard.presentation.screens.admin.action.testJackettConnection
import org.ensodai.avalonmediacard.presentation.screens.admin.action.testProwlarrConnection
import org.ensodai.avalonmediacard.presentation.screens.admin.action.testTmdbConnection
import org.ensodai.avalonmediacard.presentation.screens.admin.action.testTorrServerConnection
import org.ensodai.avalonmediacard.presentation.screens.admin.model.AdminTab
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.koin.core.annotation.Factory

@Factory
class AdminViewModel(
    private val addUserUseCase: AddUserUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val resetUserPasswordUseCase: ResetUserPasswordUseCase,
    private val getGlobalIntegrationSettingsUseCase: GetGlobalIntegrationSettingsUseCase,
    private val updateGlobalIntegrationSettingsUseCase: UpdateGlobalIntegrationSettingsUseCase,
    private val updateTmdbSettingsUseCase: UpdateTmdbSettingsUseCase,
    private val updateTorrServerSettingsUseCase: UpdateTorrServerSettingsUseCase,
    private val updateProwlarrSettingsUseCase: UpdateProwlarrSettingsUseCase,
    private val updateJackettSettingsUseCase: UpdateJackettSettingsUseCase,
    private val testTmdbConnectionUseCase: TestTmdbConnectionUseCase,
    private val testTorrServerConnectionUseCase: TestTorrServerConnectionUseCase,
    private val testProwlarrConnectionUseCase: TestProwlarrConnectionUseCase,
    private val testJackettConnectionUseCase: TestJackettConnectionUseCase,
    private val getSystemInfoUseCase: GetSystemInfoUseCase,
    private val clearDiscoverCacheUseCase: ClearDiscoverCacheUseCase,
    private val clearFeedCacheUseCase: ClearFeedCacheUseCase,
    private val clearMediaCacheUseCase: ClearMediaCacheUseCase,
    private val appSettingsStorage: AppSettingsStorage,
    private val manifestRepository: GlobalManifestRepository,
    private val tokenStorage: TokenStorage
) : BaseViewModel<AdminViewState, AdminActions>(
    initialState = AdminViewState(
        currentUserId = tokenStorage.cachedUserId,
        currentUsername = tokenStorage.cachedUsername
    )
) {

    init {
        viewModelScope.launch {
            tokenStorage.userId.collect { uid ->
                updateViewState { it.copy(currentUserId = uid) }
            }
        }
        viewModelScope.launch {
            tokenStorage.username.collect { uname ->
                updateViewState { it.copy(currentUsername = uname) }
            }
        }
        loadUsers(getUsersUseCase)
        loadGlobalIntegrations(getGlobalIntegrationSettingsUseCase)
        loadSystemInfo(getSystemInfoUseCase)
    }

    override val actions = AdminActions(
        onTabSelected = ::onTabSelected,
        onUsernameChanged = ::onUsernameChanged,
        onPasswordChanged = ::onPasswordChanged,
        onCreateUserClicked = { onCreateUserClicked(addUserUseCase) },
        clearMessages = ::clearMessages,
        loadUsers = { loadUsers(getUsersUseCase) },
        onUserStatusChange = { userId, status -> onUserStatusChange(userId, status, updateUserStatusUseCase, getUsersUseCase) },
        onUserRoleChange = { userId, role -> onUserRoleChange(userId, role, updateUserRoleUseCase, getUsersUseCase) },
        onResetUserPassword = { userId, newPassword -> onResetUserPassword(userId, newPassword, resetUserPasswordUseCase) },
        onDeleteUser = { userId -> onDeleteUser(userId, deleteUserUseCase, getUsersUseCase) },
        onTmdbTokenChanged = ::onTmdbTokenChanged,
        onTmdbShareChanged = ::onTmdbShareChanged,
        onTorrServerHostChanged = ::onTorrServerHostChanged,
        onTorrServerLoginChanged = ::onTorrServerLoginChanged,
        onTorrServerPasswordChanged = ::onTorrServerPasswordChanged,
        onTorrServerShareChanged = ::onTorrServerShareChanged,
        onTorrServerUseGstChanged = ::onTorrServerUseGstChanged,
        onProwlarrUrlChanged = ::onProwlarrUrlChanged,
        onProwlarrApiKeyChanged = ::onProwlarrApiKeyChanged,
        onProwlarrShareChanged = ::onProwlarrShareChanged,
        onTestProwlarrConnection = { testProwlarrConnection(testProwlarrConnectionUseCase) },
        onJackettUrlChanged = ::onJackettUrlChanged,
        onJackettApiKeyChanged = ::onJackettApiKeyChanged,
        onJackettShareChanged = ::onJackettShareChanged,
        onTestJackettConnection = { testJackettConnection(testJackettConnectionUseCase) },
        onTestTmdbConnection = { testTmdbConnection(testTmdbConnectionUseCase) },
        onTestTorrServerConnection = { testTorrServerConnection(testTorrServerConnectionUseCase) },
        onSaveTmdbSettings = { saveTmdbSettings(updateTmdbSettingsUseCase, manifestRepository, appSettingsStorage) },
        onSaveTorrServerSettings = { saveTorrServerSettings(updateTorrServerSettingsUseCase, appSettingsStorage) },
        onSaveProwlarrSettings = { saveProwlarrSettings(updateProwlarrSettingsUseCase, appSettingsStorage) },
        onSaveJackettSettings = { saveJackettSettings(updateJackettSettingsUseCase, appSettingsStorage) },
        loadGlobalIntegrations = { loadGlobalIntegrations(getGlobalIntegrationSettingsUseCase) },
        saveGlobalIntegrations = { saveGlobalIntegrations(updateGlobalIntegrationSettingsUseCase, manifestRepository, appSettingsStorage) },
        loadSystemInfo = { loadSystemInfo(getSystemInfoUseCase) },
        onClearDiscoverCache = { clearDiscoverCache(clearDiscoverCacheUseCase, getSystemInfoUseCase) },
        onClearFeedCache = { clearFeedCache(clearFeedCacheUseCase, getSystemInfoUseCase) },
        onClearMediaCache = { clearMediaCache(clearMediaCacheUseCase, getSystemInfoUseCase) }
    )
}
