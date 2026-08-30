package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ValidationStatus
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.TorrServerRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.ValidationStateStore
import kotlin.uuid.Uuid

class TestTorrServerConnectionUseCase(
    private val context: PluginContext,
    private val repository: TorrServerRepository,
    private val validationStore: ValidationStateStore
) {
    suspend fun execute(userId: Uuid?, host: String, login: String?, pass: String?, useGstStr: String? = null): ActionResult {
        validationStore.setValidationStatus(userId, "torrserver_host", ValidationStatus.Pending, context.i18n.t("status.checking"))
        
        if (host.isBlank()) {
            validationStore.setValidationStatus(userId, "torrserver_host", ValidationStatus.Error, context.i18n.t("status.enter_url"))
            return ActionResult.NoOp
        }
        
        val result = repository.testConnection(host, login, pass)
        var connectionResult = when (result) {
            "OK" -> ConnectionResult.Success
            "AUTH_ERROR" -> ConnectionResult.AuthError(context.i18n.t("status.auth_error"))
            else -> ConnectionResult.NetworkError(context.i18n.t("status.network_error"))
        }
        
        if (connectionResult is ConnectionResult.Success && useGstStr == "true") {
            val gstResult = repository.testGstConnection(host, login, pass)
            connectionResult = when (gstResult) {
                "OK" -> ConnectionResult.Success
                "NOT_FOUND" -> ConnectionResult.NetworkError("GST not found")
                "AUTH_ERROR" -> ConnectionResult.AuthError(context.i18n.t("status.auth_error"))
                else -> ConnectionResult.NetworkError(context.i18n.t("status.network_error"))
            }
        }
        
        return when (connectionResult) {
            is ConnectionResult.Success -> {
                validationStore.setValidationStatus(userId, "torrserver_host", ValidationStatus.Success, context.i18n.t("status.connected"))
                ActionResult.ShowNotification(context.i18n.t("notification.torrserver_success"), "success")
            }
            is ConnectionResult.AuthError -> {
                val msg = connectionResult.message ?: context.i18n.t("status.auth_error")
                validationStore.setValidationStatus(userId, "torrserver_host", ValidationStatus.Error, msg)
                ActionResult.NoOp
            }
            is ConnectionResult.NetworkError -> {
                val msg = connectionResult.message ?: context.i18n.t("status.network_error")
                validationStore.setValidationStatus(userId, "torrserver_host", ValidationStatus.Error, msg)
                ActionResult.NoOp
            }
            else -> ActionResult.NoOp
        }
    }
}
