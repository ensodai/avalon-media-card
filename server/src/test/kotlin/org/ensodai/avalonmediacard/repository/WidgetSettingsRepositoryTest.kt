package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.database.WidgetSettings
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class WidgetSettingsRepositoryTest {

    private val repository = WidgetSettingsRepository()

    @BeforeTest
    fun setup() {
        try {
            java.io.File("test_widget_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
        Database.connect("jdbc:sqlite:test_widget_avalon.db", driver = "org.sqlite.JDBC")
        transaction {
            val sql = """
                CREATE TABLE widget_settings (
                    id VARCHAR(36) PRIMARY KEY,
                    plugin_id VARCHAR(100) NOT NULL,
                    is_visible BOOLEAN NOT NULL DEFAULT 1,
                    order_index INTEGER NOT NULL DEFAULT 0,
                    width_span INTEGER NOT NULL DEFAULT 2,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                );
            """.trimIndent()
            exec(sql)
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            java.io.File("test_widget_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    @Test
    fun testSaveAndGetAllSettings() = runBlocking {
        val testId = Uuid.random()
        val setting = WidgetSettings(
            id = testId,
            pluginId = "test_widget",
            isVisible = true,
            orderIndex = 1,
            widthSpan = 3
        )

        repository.saveSetting(setting)

        val settings = repository.getAllSettings()
        assertEquals(1, settings.size)

        val retrieved = settings[0]
        assertEquals("test_widget", retrieved.pluginId)
        assertEquals(true, retrieved.isVisible)
        assertEquals(1, retrieved.orderIndex)
        assertEquals(3, retrieved.widthSpan)
    }

    @Test
    fun testSaveAllSettings() = runBlocking {
        val settings = listOf(
            WidgetSettings(Uuid.random(), "widget_1", true, 0, 4),
            WidgetSettings(Uuid.random(), "widget_2", false, 1, 2)
        )

        repository.saveAllSettings(settings)

        val retrieved = repository.getAllSettings().associateBy { it.pluginId }
        assertEquals(2, retrieved.size)

        assertEquals(true, retrieved["widget_1"]?.isVisible)
        assertEquals(4, retrieved["widget_1"]?.widthSpan)

        assertEquals(false, retrieved["widget_2"]?.isVisible)
        assertEquals(2, retrieved["widget_2"]?.widthSpan)
    }
}
