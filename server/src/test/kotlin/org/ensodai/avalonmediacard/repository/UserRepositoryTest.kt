package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.security.PasswordHasher
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*
import org.ensodai.avalonmediacard.contract.model.UserRole

class UserRepositoryTest {

    private val repository = UserRepository()

    @BeforeTest
    fun setup() {
        try {
            java.io.File("test_user_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
        Database.connect("jdbc:sqlite:test_user_avalon.db", driver = "org.sqlite.JDBC")
        transaction {
            val sqlUsers = """
                CREATE TABLE users (
                    id VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                );
            """.trimIndent()
            exec(sqlUsers)
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            java.io.File("test_user_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    @Test
    fun testPasswordHasher() {
        val password = "my_secure_password"
        val hash = PasswordHasher.hash(password)

        assertNotNull(hash)
        assertTrue(hash.startsWith("pbkdf2$"))

        assertTrue(PasswordHasher.verify(password, hash))
        assertTrue(!PasswordHasher.verify("wrong_password", hash))

        // Проверка обратной совместимости с legacy SHA-256 (salt:hash)
        val legacySalt = java.util.Base64.getEncoder().encodeToString(ByteArray(16) { 1 })
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(ByteArray(16) { 1 })
        val legacyHash = java.util.Base64.getEncoder().encodeToString(md.digest(password.toByteArray(Charsets.UTF_8)))
        val legacyStored = "$legacySalt:$legacyHash"
        assertTrue(PasswordHasher.verify(password, legacyStored))
        assertTrue(!PasswordHasher.verify("wrong_password", legacyStored))
    }

    @Test
    fun testUserLifecycle() = runBlocking {
        val username = "john_doe"
        val password = "secret_password"
        val hash = PasswordHasher.hash(password)

        val userId = repository.createUser(username, hash, UserRole.USER)
        assertNotNull(userId)

        val retrievedByUsername = repository.findByUsername(username)
        assertNotNull(retrievedByUsername)
        assertEquals(userId, retrievedByUsername.id)
        assertEquals(username, retrievedByUsername.username)
        assertEquals(UserRole.USER, retrievedByUsername.role)

        val retrievedById = repository.findById(userId)
        assertNotNull(retrievedById)
        assertEquals(username, retrievedById.username)

        val savedHash = repository.getPasswordHashByUsername(username)
        assertEquals(hash, savedHash)

        val nonExistent = repository.findByUsername("unknown")
        assertNull(nonExistent)
    }

    @Test
    fun testAdminAutoCreation() = runBlocking {
        val adminUser = "admin"
        val adminPass1 = "admin_pass_1"
        val adminPass2 = "admin_pass_2"

        // 1. Создаем админа, когда его нет в базе
        repository.createAdminIfNotExists(adminUser, adminPass1)
        val adminInfo = repository.findByUsername(adminUser)
        assertNotNull(adminInfo)
        assertEquals(UserRole.ADMIN, adminInfo.role)

        val hash1 = repository.getPasswordHashByUsername(adminUser)
        assertNotNull(hash1)
        assertTrue(PasswordHasher.verify(adminPass1, hash1))

        // 2. Повторный вызов с тем же паролем не должен ничего менять
        repository.createAdminIfNotExists(adminUser, adminPass1)
        val hash2 = repository.getPasswordHashByUsername(adminUser)
        assertEquals(hash1, hash2)

        // 3. Вызов с новым паролем должен обновить хэш
        repository.createAdminIfNotExists(adminUser, adminPass2)
        val hash3 = repository.getPasswordHashByUsername(adminUser)
        assertNotNull(hash3)
        assertTrue(hash1 != hash3)
        assertTrue(PasswordHasher.verify(adminPass2, hash3))
        assertTrue(!PasswordHasher.verify(adminPass1, hash3))
    }
}
