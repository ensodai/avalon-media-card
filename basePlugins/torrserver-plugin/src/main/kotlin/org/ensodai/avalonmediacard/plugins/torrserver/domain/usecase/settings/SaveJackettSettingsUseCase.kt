package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.SaveJackettSettingsCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SettingsRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.saveSetting
import kotlin.uuid.Uuid

class SaveJackettSettingsUseCase(
    private val context: PluginContext,
    private val repository: SettingsRepository
) {
    suspend fun execute(userId: Uuid?, cmd: SaveJackettSettingsCommand): ActionResult {
        return try {
            cmd.useJackett?.let {
                repository.saveSetting(userId, "use_jackett", it)
            }
            cmd.jackettUrl?.let {
                repository.saveSetting(userId, "jackett_url", it)
            }
            cmd.jackettApiKey?.let {
                repository.saveSetting(userId, "jackett_api_key", it)
            }
            ActionResult.ShowNotification(context.i18n.t("status.settings_saved"), "success")
        } catch (e: Exception) {
            ActionResult.Error(500, e.message ?: "Error saving settings")
        }
    }
}
