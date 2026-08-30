package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.SaveTorrServerSettingsCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SettingsRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.saveSetting
import kotlin.uuid.Uuid

class SaveTorrServerSettingsUseCase(
    private val context: PluginContext,
    private val repository: SettingsRepository
) {
    suspend fun execute(userId: Uuid?, cmd: SaveTorrServerSettingsCommand): ActionResult {
        return try {
            cmd.useTorrServer?.let {
                repository.saveSetting(userId, "use_torrserver", it)
            }
            cmd.torrserverHost?.let {
                repository.saveSetting(userId, "torrserver_host", it)
            }
            cmd.torrserverLogin?.let {
                repository.saveSetting(userId, "torrserver_login", it)
            }
            cmd.torrserverPassword?.let {
                repository.saveSetting(userId, "torrserver_password", it)
            }
            cmd.useTorrServerGst?.let {
                repository.saveSetting(userId, "use_torrserver_gst", it)
            }
            ActionResult.ShowNotification(context.i18n.t("status.settings_saved"), "success")
        } catch (e: Exception) {
            ActionResult.Error(500, e.message ?: "Error saving settings")
        }
    }
}
