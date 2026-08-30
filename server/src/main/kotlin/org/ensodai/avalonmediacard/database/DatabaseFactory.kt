package org.ensodai.avalonmediacard.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.sqlite.SQLiteConfig

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    var isSqlite: Boolean = true
        private set

    fun init() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            return
        }

        val hikariConfig = HikariConfig().apply {
            val dbUrl = System.getenv("DB_URL") ?: "jdbc:sqlite:avalon.db"
            jdbcUrl = dbUrl
            isSqlite = dbUrl.startsWith("jdbc:sqlite:")

            if (!isSqlite) {
                driverClassName = "org.postgresql.Driver"
                username = System.getenv("DB_USER") ?: "postgres"
                password = System.getenv("DB_PASSWORD") ?: ""
                maximumPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10
            } else {
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10
                minimumIdle = 1
                connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=30000; PRAGMA synchronous=NORMAL; PRAGMA foreign_keys=ON;"

                val sqliteConfig = SQLiteConfig().apply {
                    setJournalMode(SQLiteConfig.JournalMode.WAL)
                    setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
                    setBusyTimeout(30000)
                    enforceForeignKeys(true)
                }
                dataSourceProperties = sqliteConfig.toProperties().apply {
                    setProperty("mmap_size", "30000000000") // 30GB mmap
                    setProperty("cache_size", "-10000")     // 10MB cache
                }
            }
            validate()
        }

        // 1. Создаем DataSource для запуска Flyway миграций (1 соединение для безопасного DDL)
        val flywayHikariConfig = HikariConfig().apply {
            jdbcUrl = hikariConfig.jdbcUrl
            driverClassName = hikariConfig.driverClassName
            maximumPoolSize = 1
            connectionInitSql = hikariConfig.connectionInitSql
            hikariConfig.dataSourceProperties.forEach { (key, value) ->
                addDataSourceProperty(key.toString(), value)
            }
        }
        val flywayDataSource = HikariDataSource(flywayHikariConfig)

        try {
            val flyway = Flyway.configure()
                .dataSource(flywayDataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
            flyway.repair()
            flyway.migrate()
        } catch (e: Exception) {
            println("Flyway миграция упала: ${e.message}")
            throw e
        } finally {
            flywayDataSource.close()
        }

        // 2. Создаем DataSource для Exposed
        val exposedHikariConfig = HikariConfig().apply {
            jdbcUrl = hikariConfig.jdbcUrl
            driverClassName = hikariConfig.driverClassName
            maximumPoolSize = hikariConfig.maximumPoolSize
            minimumIdle = hikariConfig.minimumIdle
            connectionInitSql = hikariConfig.connectionInitSql
            isAutoCommit = true
            hikariConfig.dataSourceProperties.forEach { (key, value) ->
                addDataSourceProperty(key.toString(), value)
            }
        }

        dataSource = HikariDataSource(exposedHikariConfig)
        Database.connect(dataSource)

        // 3. Проверка схемы и создание триггеров
        transaction {
            val createStatements = MigrationUtils.statementsRequiredForDatabaseMigration(*AllDatabaseTables)
            val dropStatements = MigrationUtils.dropUnmappedColumnsStatements(*AllDatabaseTables)
            val allStatements = createStatements + dropStatements

            val criticalStatements = allStatements.filter { it.startsWith("CREATE ") || it.startsWith("ALTER ") }

            if (criticalStatements.isNotEmpty()) {
                val errorMsg =
                    "Критическая ошибка старта: Схема БД (Flyway) не совпадает с кодом (Exposed Table)! Отсутствуют миграции для:\n" +
                            criticalStatements.joinToString("\n")
                System.err.println(errorMsg)
                throw IllegalStateException(errorMsg)
            }

            setupTriggers()
        }
    }

    private fun setupTriggers() {
        if (!isSqlite) return
        val existingTriggers = mutableSetOf<String>()
        TransactionManager.current().exec("SELECT name FROM sqlite_master WHERE type='trigger';") { rs ->
            while (rs.next()) {
                existingTriggers.add(rs.getString("name"))
            }
        }

        val tables = mutableListOf<String>()
        TransactionManager.current().exec(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'flyway_%';"
        ) { rs ->
            while (rs.next()) {
                tables.add(rs.getString("name"))
            }
        }

        for (table in tables) {
            val triggerName = "trg_update_${table}_updated_at"
            if (triggerName in existingTriggers) continue

            var hasUpdatedAt = false
            var pkColumn = "id"
            TransactionManager.current().exec("PRAGMA table_info($table);") { rs ->
                while (rs.next()) {
                    val columnName = rs.getString("name")
                    if (columnName == "updated_at") {
                        hasUpdatedAt = true
                    }
                    if (rs.getInt("pk") > 0) {
                        pkColumn = columnName
                    }
                }
            }

            if (hasUpdatedAt) {
                val triggerSql = """
                    CREATE TRIGGER IF NOT EXISTS $triggerName
                    AFTER UPDATE ON $table
                    FOR EACH ROW
                    WHEN (NEW.updated_at IS OLD.updated_at OR NEW.updated_at IS NULL)
                    BEGIN
                        UPDATE $table
                        SET updated_at = CURRENT_TIMESTAMP
                        WHERE $pkColumn = OLD.$pkColumn;
                    END;
                """.trimIndent()
                TransactionManager.current().exec(triggerSql) { }
            }
        }
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}

private val sqliteDbMutex = Mutex()

/**
 * Выполняет блокирующий вызов к базе данных внутри транзакции,
 * безопасно переключая контекст корутины на Dispatchers.IO.
 * Для SQLite гарантирует отсутствие конфликтов параллельной записи ([SQLITE_BUSY]).
 */
suspend fun <T> dbQuery(
    block: suspend Transaction.() -> T
): T = withContext(Dispatchers.IO) {
    if (DatabaseFactory.isSqlite) {
        sqliteDbMutex.withLock {
            suspendTransaction {
                block()
            }
        }
    } else {
        suspendTransaction {
            block()
        }
    }
}
