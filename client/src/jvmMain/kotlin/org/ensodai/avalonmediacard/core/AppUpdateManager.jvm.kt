package org.ensodai.avalonmediacard.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class JvmNoOpAppUpdateManager : AppUpdateManager {
    override val isUpdateAvailable: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val newVersionName: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    override fun checkForUpdates() {}
    override fun applyUpdate() {}
    override fun dismissUpdate() {}
}

actual fun createPlatformUpdateManager(): AppUpdateManager = JvmNoOpAppUpdateManager()
