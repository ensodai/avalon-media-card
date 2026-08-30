package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.UserMediaBindingTable
import org.ensodai.avalonmediacard.database.UserTable
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*
import kotlin.uuid.Uuid

class UserMediaBindingRepositoryTest {

    private val repository = UserMediaBindingRepositoryImpl()
    private val testDbFile = java.io.File("test_binding_avalon.db")

    @BeforeTest
    fun setup() {
        testDbFile.delete()
        Database.connect("jdbc:sqlite:test_binding_avalon.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(UserTable, MediaTable, UserMediaBindingTable)
        }
    }

    @AfterTest
    fun tearDown() {
        testDbFile.delete()
    }

    @Test
    fun testSaveLongSourceIdBinding() = runBlocking {
        val testUserId = Uuid.random()
        val testMediaId = "tmdb_123456"

        transaction {
            UserTable.insert {
                it[id] = testUserId
                it[username] = "testuser"
                it[passwordHash] = "hash"
            }
            MediaTable.insert {
                it[id] = Uuid.random()
                it[catalogId] = "tmdb"
                it[externalId] = testMediaId
                it[mediaType] = "movie"
            }
        }

        // Magnet link length > 256 characters (e.g. 1000+ chars)
        val longMagnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Test+Torrent+Title+With+Very+Long+Description+And+Trackers" +
                "&tr=http%3A%2F%2Ftracker.example.com%3A80%2Fannounce".repeat(20)
        assertTrue(longMagnet.length > 256, "Magnet length should be > 256")

        repository.saveBinding(
            userId = testUserId,
            mediaId = testMediaId,
            sourceType = "torrserver",
            sourceId = longMagnet
        )

        val saved = repository.getBinding(testUserId, testMediaId, "torrserver")
        assertNotNull(saved)
        assertEquals(longMagnet, saved)

        val active = repository.getActiveBinding(testUserId, testMediaId)
        assertNotNull(active)
        assertEquals(longMagnet, active.sourceId)
        assertEquals("torrserver", active.sourceType)
    }
}
