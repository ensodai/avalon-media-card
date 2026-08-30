package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.SaveProwlarrSettingsCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SettingsRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.saveSetting
import kotlin.uuid.Uuid

class SaveProwlarrSettingsUseCase(
    private val context: PluginContext,
    private val repository: SettingsRepository
) {
    suspend fun execute(userId: Uuid?, cmd: SaveProwlarrSettingsCommand): ActionResult {
        return try {
            cmd.useProwlarr?.let {
                repository.saveSetting(userId, "use_prowlarr", it)
            }
            cmd.prowlarrUrl?.let {
                repository.saveSetting(userId, "prowlarr_url", it)
            }
            cmd.prowlarrApiKey?.let {
                repository.saveSetting(userId, "prowlarr_api_key", it)
            }
            ActionResult.ShowNotification(context.i18n.t("status.settings_saved"), "success")
        } catch (e: Exception) {
            ActionResult.Error(500, e.message ?: "Error saving settings")
        }
    }
}
