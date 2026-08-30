package org.ensodai.avalonmediacard.auth


import kotlin.uuid.Uuid

interface IntegrationProvider {
    val serviceName: String

    suspend fun getSettingsDialog(userId: Uuid): org.ensodai.avalonmediacard.contract.slot.Action?

    suspend fun saveSettings(userId: Uuid, settingsJson: String): Boolean

    suspend fun triggerSync(userId: Uuid): Boolean
}
