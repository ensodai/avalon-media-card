package org.ensodai.avalonmediacard.auth

import app.moviebase.trakt.Trakt
import app.moviebase.trakt.model.*
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

import org.ensodai.avalonmediacard.utils.EnvHelper

/**
 * Полный цикл: удалить список -> создать -> добавить фильм -> проверить что фильм есть.
 * Фильм: The Godfather (tmdb=238)
 * Список: "тест 3"
 */
@Ignore
class TraktAddItemTest {

    private val clientId = EnvHelper.getEnv("TRAKT_CLIENT_ID") ?: ""
    private val accessToken = EnvHelper.getEnv("TRAKT_ACCESS_TOKEN") ?: ""
    private val testListName = "тест 3"
    private val testTmdbId = 238 // The Godfather

    private fun createTrakt(): Trakt = Trakt {
        this.clientId = this@TraktAddItemTest.clientId
        userAuthentication {
            loadTokens {
                BearerTokens(accessToken, "")
            }
        }
    }

    @Test
    fun `полный цикл - создать список, добавить фильм, проверить`() = runBlocking {
        val trakt = createTrakt()

        // 1. Удаляем список "тест 3" если он существует
        println("=== ШАГ 1: Удаление старого списка '$testListName' ===")
        val existingLists = trakt.users.getLists(TraktUserSlug.ME)
        println("  Текущие списки: ${existingLists.map { "'${it.name}' (slug=${it.ids?.slug})" }}")
        val existing = existingLists.find { it.name == testListName }
        if (existing != null) {
            val slug = existing.ids?.slug
            println("  Найден список '$testListName' со slug='$slug', удаляем...")
            trakt.users.deleteList(TraktUserSlug.ME, slug!!)
            println("  Удалён.")
        } else {
            println("  Список '$testListName' не найден, пропускаем удаление.")
        }

        // 2. Создаём новый список
        println("\n=== ШАГ 2: Создание нового списка '$testListName' ===")
        val newList = trakt.users.createList(
            TraktUserSlug.ME,
            TraktList(name = testListName, privacy = TraktListPrivacy.PRIVATE)
        )
        val newSlug = newList.ids?.slug
        val newTraktId = newList.ids?.trakt
        println("  Создан: name='${newList.name}', trakt_id=$newTraktId, slug='$newSlug'")

        // Будем использовать trakt_id (числовой) вместо slug, т.к. slug "3" конфликтует с глобальным trakt_id=3
        val listIdForApi = newTraktId.toString()

        // 3. Добавляем фильм The Godfather (tmdb=238)
        println("\n=== ШАГ 3: Добавление фильма tmdb=$testTmdbId в список trakt_id=$listIdForApi ===")
        val syncItems = TraktSyncItems(
            movies = listOf(TraktSyncMovie(ids = TraktItemIds(tmdb = testTmdbId)))
        )
        val addResponse = trakt.users.addListItems(TraktUserSlug.ME, listIdForApi, syncItems)
        println("  Ответ Тракта: added=${addResponse.added}, notFound=${addResponse.notFound}")

        // 4. Читаем содержимое списка
        println("\n=== ШАГ 4: Проверка содержимого списка trakt_id=$listIdForApi ===")
        val items = trakt.users.getListItems(TraktUserSlug.ME, listIdForApi)
        println("  Items (${items.size} шт.):")
        for (item in items) {
            val movie = item.movie
            val title = movie?.title ?: "?"
            val tmdb = movie?.ids?.tmdb
            println("    - ${item.type}: $title (tmdb=$tmdb)")
        }

        // 5. Проверяем что фильм есть
        val found = items.any { it.movie?.ids?.tmdb == testTmdbId }
        println("\n=== РЕЗУЛЬТАТ: The Godfather (tmdb=$testTmdbId) найден = $found ===")
        assertTrue(found, "Фильм The Godfather (tmdb=$testTmdbId) должен быть в списке '$testListName'")
    }
}
