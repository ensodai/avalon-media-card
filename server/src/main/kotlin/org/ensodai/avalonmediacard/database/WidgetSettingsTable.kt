package org.ensodai.avalonmediacard.database

import kotlin.uuid.Uuid

object WidgetSettingsTable : BaseUuidTable("widget_settings") {
    val pluginId = varchar("plugin_id", 100)
    val isVisible = bool("is_visible")
    val orderIndex = integer("order_index")
    val widthSpan = integer("width_span").default(2)
}

data class WidgetSettings(
    val id: Uuid,
    val pluginId: String,
    val isVisible: Boolean,
    val orderIndex: Int,
    val widthSpan: Int = 2
)
