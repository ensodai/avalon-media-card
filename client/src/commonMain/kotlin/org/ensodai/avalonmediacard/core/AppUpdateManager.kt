package org.ensodai.avalonmediacard.core

import kotlinx.coroutines.flow.StateFlow

interface AppUpdateManager {
    val isUpdateAvailable: StateFlow<Boolean>
    val newVersionName: StateFlow<String?>
    fun checkForUpdates()
    fun applyUpdate()
    fun dismissUpdate()
}

expect fun createPlatformUpdateManager(): AppUpdateManager
