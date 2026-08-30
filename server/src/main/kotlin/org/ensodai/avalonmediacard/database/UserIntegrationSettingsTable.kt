package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.ReferenceOption

object UserIntegrationSettingsTable : BaseUuidTable("user_integration_settings") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val pluginId = varchar("plugin_id", 128)
    val settingKey = varchar("setting_key", 255)
    val settingValue = text("setting_value").nullable()
}
