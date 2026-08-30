package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.ensodai.avalonmediacard.database.WidgetSettings
import org.ensodai.avalonmediacard.database.WidgetSettingsTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class WidgetSettingsRepository {
    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val updates = _updates.asSharedFlow()

    suspend fun getAllSettings(): List<WidgetSettings> = dbQuery {
        WidgetSettingsTable.selectAll()
            .orderBy(WidgetSettingsTable.orderIndex to SortOrder.ASC)
            .map {
                WidgetSettings(
                    id = it[WidgetSettingsTable.id].value,
                    pluginId = it[WidgetSettingsTable.pluginId],
                    isVisible = it[WidgetSettingsTable.isVisible],
                    orderIndex = it[WidgetSettingsTable.orderIndex],
                    widthSpan = it[WidgetSettingsTable.widthSpan]
                )
            }
    }

    suspend fun saveSetting(setting: WidgetSettings) = dbQuery {
        val exists = WidgetSettingsTable.selectAll()
            .where { WidgetSettingsTable.pluginId eq setting.pluginId }
            .any()
        if (exists) {
            WidgetSettingsTable.update({ WidgetSettingsTable.pluginId eq setting.pluginId }) {
                it[isVisible] = setting.isVisible
                it[orderIndex] = setting.orderIndex
                it[widthSpan] = setting.widthSpan
            }
        } else {
            WidgetSettingsTable.insert {
                it[id] = setting.id
                it[pluginId] = setting.pluginId
                it[isVisible] = setting.isVisible
                it[orderIndex] = setting.orderIndex
                it[widthSpan] = setting.widthSpan
            }
        }
        _updates.emit(Unit)
    }

    suspend fun saveAllSettings(settings: List<WidgetSettings>) = dbQuery {
        for (setting in settings) {
            val exists = WidgetSettingsTable.selectAll()
                .where { WidgetSettingsTable.pluginId eq setting.pluginId }
                .any()
            if (exists) {
                WidgetSettingsTable.update({ WidgetSettingsTable.pluginId eq setting.pluginId }) {
                    it[isVisible] = setting.isVisible
                    it[orderIndex] = setting.orderIndex
                    it[widthSpan] = setting.widthSpan
                }
            } else {
                WidgetSettingsTable.insert {
                    it[id] = setting.id
                    it[pluginId] = setting.pluginId
                    it[isVisible] = setting.isVisible
                    it[orderIndex] = setting.orderIndex
                    it[widthSpan] = setting.widthSpan
                }
            }
        }
        _updates.emit(Unit)
    }

    suspend fun updateVisibility(id: Uuid, isVisible: Boolean) = dbQuery {
        WidgetSettingsTable.update({ WidgetSettingsTable.id eq id }) {
            it[WidgetSettingsTable.isVisible] = isVisible
        }
        _updates.emit(Unit)
    }

    suspend fun updateOrder(id: Uuid, orderIndex: Int) = dbQuery {
        WidgetSettingsTable.update({ WidgetSettingsTable.id eq id }) {
            it[WidgetSettingsTable.orderIndex] = orderIndex
        }
        _updates.emit(Unit)
    }
}

