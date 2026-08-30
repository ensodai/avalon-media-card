package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.sync.SyncAction
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.ensodai.avalonmediacard.contract.sync.UserMediaSyncQueueItem
import org.ensodai.avalonmediacard.database.AllDatabaseTables
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class UserMovieRepositoryTest {

    private val repository = UserMovieRepository()
    private val userId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    @BeforeTest
    fun setup() {
        try {
            java.io.File("test_user_movie_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
        Database.connect("jdbc:sqlite:test_user_movie_avalon.db", driver = "org.sqlite.JDBC")
        transaction {
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*AllDatabaseTables)
            for (stmt in statements) {
                exec(stmt)
            }
            exec("INSERT INTO users (id, username, password_hash, role) VALUES ('$userId', 'testuser', 'hash', 'USER')")
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            java.io.File("test_user_movie_avalon.db").delete()
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    @Test
    fun testUpdateAndGetUserMovies() = runBlocking {
        val item = UserMovieItem(
            id = Uuid.random(),
            userId = userId,
            catalogId = "tmdb",
            mediaId = "123",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.WATCHING,
            userRating = 8,
            progressSeconds = 300,
            durationSeconds = 9000,
            inCollection = true,
            lastWatchedAt = Instant.fromEpochMilliseconds(1000000L)
        )

        val updated = repository.updateUserMovie(item)
        assertTrue(updated, "Movie progress should be updated successfully")

        val movies = repository.getUserMovies(userId)
        assertEquals(1, movies.size)

        val retrieved = movies[0]
        assertEquals("123", retrieved.mediaId)
        assertEquals("tmdb", retrieved.catalogId)
        assertEquals(MediaType.MOVIE, retrieved.mediaType)
        assertEquals(MediaStatus.WATCHING, retrieved.status)
        assertEquals(8, retrieved.userRating)
        assertEquals(300L, retrieved.progressSeconds)
        assertEquals(true, retrieved.inCollection)
        assertEquals(Instant.fromEpochMilliseconds(1000000L), retrieved.lastWatchedAt)
    }

    @Test
    fun testDeleteUserMovie() = runBlocking {
        val item = UserMovieItem(
            id = Uuid.random(),
            userId = userId,
            catalogId = "tmdb",
            mediaId = "123",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.WATCHING,
            userRating = null,
            progressSeconds = 300,
            durationSeconds = 9000,
            lastWatchedAt = Instant.fromEpochMilliseconds(1000000L)
        )

        repository.updateUserMovie(item)
        val removed = repository.deleteUserMovie(userId, "123")
        assertTrue(removed, "Movie should be removed successfully")

        val movies = repository.getUserMovies(userId)
        assertEquals(0, movies.size)
    }

    @Test
    fun testUpdateAndGetUserEpisodes() = runBlocking {
        transaction {
            val showId = Uuid.random()
            val seasonId = Uuid.random()
            val episodeId = Uuid.random()
            org.ensodai.avalonmediacard.database.MediaTable.insert {
                it[id] = showId
                it[catalogId] = "tmdb"
                it[externalId] = "456"
                it[mediaType] = "TV"
            }
            org.ensodai.avalonmediacard.database.MediaSeasonTable.insert {
                it[id] = seasonId
                it[mediaId] = showId
                it[seasonNumber] = 1
            }
            org.ensodai.avalonmediacard.database.MediaEpisodeTable.insert {
                it[id] = episodeId
                it[this.seasonId] = seasonId
                it[episodeNumber] = 2
            }
        }

        val episode = UserEpisodeItem(
            id = Uuid.random(),
            userId = userId,
            catalogId = "tmdb",
            mediaId = "456",
            season = 1,
            episode = 2,
            progressSeconds = 600,
            durationSeconds = 2400,
            isWatched = false,
            inCollection = true,
            lastWatchedAt = Instant.fromEpochMilliseconds(2000000L)
        )

        val updated = repository.updateUserEpisode(episode)
        assertTrue(updated, "Episode progress should be updated successfully")

        val episodes = repository.getUserEpisodes(userId, "456")
        assertEquals(1, episodes.size)

        val retrieved = episodes[0]
        assertEquals(1, retrieved.season)
        assertEquals(2, retrieved.episode)
        assertEquals(600L, retrieved.progressSeconds)
        assertEquals(2400L, retrieved.durationSeconds)
        assertEquals(false, retrieved.isWatched)
        assertEquals(true, retrieved.inCollection)
        assertEquals(Instant.fromEpochMilliseconds(2000000L), retrieved.lastWatchedAt)
    }

    @Test
    fun testSyncQueue() = runBlocking {
        transaction {
            val mediaId = Uuid.random()
            org.ensodai.avalonmediacard.database.MediaTable.insert {
                it[id] = mediaId
                it[catalogId] = "tmdb"
                it[externalId] = "123"
                it[mediaType] = "MOVIE"
            }
        }

        val item = UserMediaSyncQueueItem(
            id = Uuid.random(),
            userId = userId,
            mediaType = MediaType.MOVIE,
            mediaId = "123",
            service = "trakt",
            action = SyncAction.WATCH,
            status = SyncStatus.PENDING,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        val added = repository.addToSyncQueue(item)
        assertTrue(added, "Should successfully add to sync queue")

        val pending = repository.getPendingSyncItems()
        assertEquals(1, pending.size)
        assertEquals("123", pending[0].mediaId)
        assertEquals(SyncAction.WATCH, pending[0].action)
        assertEquals(SyncStatus.PENDING, pending[0].status)

        val updated = repository.updateSyncItemStatus(item.id, SyncStatus.SUCCESS, 1, Clock.System.now())
        assertTrue(updated, "Should successfully update sync status")

        val pendingAfter = repository.getPendingSyncItems()
        assertEquals(0, pendingAfter.size)
    }

    @Test
    fun testShowProgressCaching() = runBlocking {
        val showTmdbId = 998877
        val progress = org.ensodai.avalonmediacard.auth.WatchedProgress(
            season = 2,
            number = 5,
            title = "Episode Title",
            tmdbId = 112233
        )

        // 1. Initially it should be null
        val initial = repository.getCachedShowWatchedProgress(userId, showTmdbId)
        assertEquals(null, initial)

        // 2. Save it
        repository.saveCachedShowWatchedProgress(userId, showTmdbId, progress)

        // 3. Retrieve it
        val cached = repository.getCachedShowWatchedProgress(userId, showTmdbId)
        assertTrue(cached != null, "Cached progress should not be null")
        assertEquals(2, cached.progress.season)
        assertEquals(5, cached.progress.number)
        assertEquals("Episode Title", cached.progress.title)
        assertEquals(112233, cached.progress.tmdbId)

        // 4. Update it
        val updatedProgress = progress.copy(number = 6, title = "Next Episode")
        repository.saveCachedShowWatchedProgress(userId, showTmdbId, updatedProgress)

        val retrievedUpdated = repository.getCachedShowWatchedProgress(userId, showTmdbId)
        assertTrue(retrievedUpdated != null)
        assertEquals(6, retrievedUpdated.progress.number)
        assertEquals("Next Episode", retrievedUpdated.progress.title)

        // 5. Delete it by passing null
        repository.saveCachedShowWatchedProgress(userId, showTmdbId, null)
        val afterDelete = repository.getCachedShowWatchedProgress(userId, showTmdbId)
        assertEquals(null, afterDelete)
    }
}
