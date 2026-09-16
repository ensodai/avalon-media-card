package org.ensodai.avalonmediacard.plugins.samsungtv

import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.plugins.samsungtv.domain.RegisterSamsungWidgetCommand
import org.ensodai.avalonmediacard.plugins.samsungtv.domain.SamsungWidgetRegistrar
import org.ensodai.avalonmediacard.plugins.samsungtv.domain.SaveSamsungTvSettingsCommand
import org.ensodai.avalonmediacard.plugins.samsungtv.presentation.SamsungTvScreenRegistry
import org.ensodai.avalonmediacard.plugins.samsungtv.presentation.SamsungTvSettingsPresenter

/**
 * Drop-in plugin: registers a Tizen widget for Samsung Smart TV.
 * Publishes `.wgt`, URL Launcher files and Smart Hub preview into `WEB_DIR`
 * so the existing static file server exposes them — no core route is required.
 */
class SamsungTvPlugin : AvalonPlugin {
    override val id: String = SamsungTvPaths.PLUGIN_ID
    override val name: String = "Samsung TV Widget"
    override val version: String = "1.1.0"
    override val author: String = "Avalon Media Card"

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(SaveSamsungTvSettingsCommand::class)
            subclass(RegisterSamsungWidgetCommand::class)
        }
        polymorphic(ServerAction::class) {
            subclass(SaveSamsungTvSettingsCommand::class)
            subclass(RegisterSamsungWidgetCommand::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        val widgetRegistrar = SamsungWidgetRegistrar(context, version)

        runCatching { widgetRegistrar.publishBundledResources() }
            .onFailure { context.logger.error("Failed to publish Samsung TV widget files", it) }

        context.scope.launch {
            runCatching { widgetRegistrar.register() }
                .onFailure { context.logger.error("Failed to register Samsung TV widget", it) }
        }

        val settingsPresenter = SamsungTvSettingsPresenter(id, context)
        SamsungTvScreenRegistry.register(context, id, settingsPresenter)

        context.actions.bind<SaveSamsungTvSettingsCommand> { cmd, userId ->
            cmd.widgetEnabled?.let {
                context.settings.setBoolean(SamsungTvPaths.SETTING_ENABLED, it.equals("true", ignoreCase = true))
            }
            cmd.publicBaseUrl?.let {
                context.settings.setString(SamsungTvPaths.SETTING_PUBLIC_URL, SamsungTvPaths.normalizeBaseUrl(it))
            }
            if (userId != null) {
                context.settings.setString(SamsungTvPaths.SETTING_PREVIEW_USER, userId.toString())
            }
            try {
                widgetRegistrar.register(userId)
                ActionResult.ShowNotification(context.i18n.t("status.widget_registered"), "success")
            } catch (e: Exception) {
                context.logger.error("Failed to refresh Samsung TV widget", e)
                ActionResult.Error(500, e.message ?: "Failed to register Samsung TV widget")
            }
        }

        context.actions.bind<RegisterSamsungWidgetCommand> { _, userId ->
            try {
                widgetRegistrar.register(userId)
                ActionResult.ShowNotification(context.i18n.t("status.widget_registered"), "success")
            } catch (e: Exception) {
                context.logger.error("Failed to register Samsung TV widget", e)
                ActionResult.Error(500, e.message ?: "Failed to register Samsung TV widget")
            }
        }

        context.logger.info("Samsung TV widget plugin initialized")
    }
}
