package org.ensodai.avalonmediacard.presentation.screens.admin.action

import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions
import org.ensodai.avalonmediacard.presentation.screens.admin.model.AdminTab

data class AdminActions(
    val onTabSelected: (AdminTab) -> Unit,
    val onUsernameChanged: (String) -> Unit,
    val onPasswordChanged: (String) -> Unit,
    val onCreateUserClicked: () -> Unit,
    val clearMessages: () -> Unit,
    val loadUsers: () -> Unit,
    val onUserStatusChange: (String, UserStatus) -> Unit,
    val onUserRoleChange: (String, UserRole) -> Unit,
    val onResetUserPassword: (String, String) -> Unit,
    val onDeleteUser: (String) -> Unit,
    
    // Integrations Tab
    val onTmdbTokenChanged: (String) -> Unit,
    val onTmdbShareChanged: (Boolean) -> Unit,
    val onTorrServerHostChanged: (String) -> Unit,
    val onTorrServerLoginChanged: (String) -> Unit,
    val onTorrServerPasswordChanged: (String) -> Unit,
    val onTorrServerShareChanged: (Boolean) -> Unit,
    val onTorrServerUseGstChanged: (Boolean) -> Unit,
    val onProwlarrUrlChanged: (String) -> Unit,
    val onProwlarrApiKeyChanged: (String) -> Unit,
    val onProwlarrShareChanged: (Boolean) -> Unit,
    val onTestProwlarrConnection: () -> Unit,
    val onJackettUrlChanged: (String) -> Unit,
    val onJackettApiKeyChanged: (String) -> Unit,
    val onJackettShareChanged: (Boolean) -> Unit,
    val onTestJackettConnection: () -> Unit,
    val onTestTmdbConnection: () -> Unit,
    val onTestTorrServerConnection: () -> Unit,
    val onSaveTmdbSettings: () -> Unit,
    val onSaveTorrServerSettings: () -> Unit,
    val onSaveProwlarrSettings: () -> Unit,
    val onSaveJackettSettings: () -> Unit,
    val loadGlobalIntegrations: () -> Unit,
    val saveGlobalIntegrations: () -> Unit,

    // System Tab
    val loadSystemInfo: () -> Unit,
    val onClearDiscoverCache: () -> Unit,
    val onClearFeedCache: () -> Unit,
    val onClearMediaCache: () -> Unit,
    val onReloadPlugins: () -> Unit
) : BaseActions()
