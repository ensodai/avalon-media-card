package org.ensodai.avalonmediacard.auth

import app.moviebase.trakt.Trakt
import app.moviebase.trakt.model.TraktUserSlug
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

import org.ensodai.avalonmediacard.utils.EnvHelper

/**
 * Интеграционный тест для проверки работы с Trakt API через библиотеку trakt-kotlin.
 * Использует переменные окружения TRAKT_CLIENT_ID и TRAKT_ACCESS_TOKEN из .env.
 */
@Ignore
class TraktListsIntegrationTest {

    private val clientId = EnvHelper.getEnv("TRAKT_CLIENT_ID") ?: ""
    private val accessToken = EnvHelper.getEnv("TRAKT_ACCESS_TOKEN") ?: ""

    private fun createTrakt(): Trakt = Trakt {
        this.clientId = this@TraktListsIntegrationTest.clientId
        userAuthentication {
            loadTokens {
                BearerTokens(accessToken, "")
            }
        }
    }

    @Test
    fun `проверить все списки и их содержимое`() = runBlocking {
        val trakt = createTrakt()

        // 1. Получаем все списки пользователя
        val lists = trakt.users.getLists(TraktUserSlug.ME)
        println("=== СПИСКИ ПОЛЬЗОВАТЕЛЯ (${lists.size} шт.) ===")
        for (list in lists) {
            val traktId = list.ids?.trakt
            val slug = list.ids?.slug
            println("  Список: '${list.name}' | trakt_id=$traktId | slug='$slug'")

            // 2. Для каждого списка получаем items
            if (slug != null) {
                try {
                    val items = trakt.users.getListItems(TraktUserSlug.ME, slug)
                    println("    -> Items (${items.size} шт.):")
                    for (item in items) {
                        val type = item.type
                        val movie = item.movie
                        val show = item.show
                        val title = movie?.title ?: show?.title ?: "?"
                        val tmdbId = movie?.ids?.tmdb ?: show?.ids?.tmdb
                        println("      - [$type] $title (tmdb=$tmdbId)")
                    }
                    if (items.isEmpty()) {
                        println("      (пусто)")
                    }
                } catch (e: Exception) {
                    println("    -> ОШИБКА при получении items: ${e.message}")
                }
            }
        }
        println("=== КОНЕЦ ===")
    }
}
