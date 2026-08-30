package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings

import io.ktor.client.*
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ValidationStatus
import org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers.JackettClient
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.ValidationStateStore
import kotlin.uuid.Uuid

class TestJackettConnectionUseCase(
    private val context: PluginContext,
    private val httpClient: HttpClient,
    private val validationStore: ValidationStateStore
) {
    suspend fun execute(userId: Uuid?, url: String, apiKey: String): ActionResult {
        validationStore.setValidationStatus(userId, "jackett_url", ValidationStatus.Pending, context.i18n.t("status.checking"))
        
        if (url.isBlank()) {
            validationStore.setValidationStatus(userId, "jackett_url", ValidationStatus.Error, context.i18n.t("status.enter_url"))
            return ActionResult.NoOp
        }
        
        val result = JackettClient.testConnection(httpClient, url, apiKey)
        return when (result) {
            is ConnectionResult.Success -> {
                validationStore.setValidationStatus(userId, "jackett_url", ValidationStatus.Success, context.i18n.t("status.connected"))
                ActionResult.ShowNotification(context.i18n.t("notification.jackett_success"), "success")
            }
            is ConnectionResult.AuthError -> {
                val msg = result.message ?: context.i18n.t("status.auth_error")
                validationStore.setValidationStatus(userId, "jackett_url", ValidationStatus.Error, msg)
                ActionResult.NoOp
            }
            is ConnectionResult.NetworkError -> {
                val msg = result.message ?: context.i18n.t("status.network_error")
                validationStore.setValidationStatus(userId, "jackett_url", ValidationStatus.Error, msg)
                ActionResult.NoOp
            }
            else -> ActionResult.NoOp
        }
    }
}
