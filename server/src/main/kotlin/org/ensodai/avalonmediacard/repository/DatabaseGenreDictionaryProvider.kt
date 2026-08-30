package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider
import org.ensodai.avalonmediacard.database.MediaGenreDictionaryTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Single(binds = [GenreDictionaryProvider::class])
class DatabaseGenreDictionaryProvider(
    private val tmdbApi: TmdbApi
) : GenreDictionaryProvider {

    private val logger = LoggerFactory.getLogger(DatabaseGenreDictionaryProvider::class.java)

    // In-memory кэш словарей по языкам (например, "ru-RU" -> { "28" -> "Экшен" })
    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    private val syncMutex = Mutex()

    override suspend fun getLocalizedGenres(language: String): Map<String, String> {
        val normLang = normalizeCode(language)
        // 1. Проверяем in-memory кэш
        cache[normLang]?.let { return it }

        // 2. Если в кэше нет, используем мьютекс, чтобы не запустить 10 параллельных запросов в TMDB
        return syncMutex.withLock {
            // Двойная проверка (DCL)
            cache[normLang]?.let { return@withLock it }

            // 3. Пытаемся достать из локальной базы данных
            var dbGenres = fetchFromDb(normLang)

            // 4. Если в базе пусто, идем в TMDB, выкачиваем и сохраняем
            if (dbGenres.isEmpty()) {
                logger.info("Local DB has no genres for $normLang. Fetching from TMDB API...")
                val tmdbLang = if (normLang == "ru") "ru-RU" else "en-US"
                fetchAndSaveFromTmdb(tmdbLang, normLang)

                // Перечитываем из базы после сохранения
                dbGenres = fetchFromDb(normLang)
            }

            // 5. Сохраняем в in-memory кэш
            val result = dbGenres.associate { it.first.toString() to it.second }
            if (result.isNotEmpty()) {
                cache[normLang] = result
            }

            result
        }
    }

    private suspend fun fetchFromDb(languageCode: String): List<Pair<Int, String>> {
        return dbQuery {
            MediaGenreDictionaryTable
                .selectAll().where { MediaGenreDictionaryTable.languageCode eq languageCode }
                .map {
                    it[MediaGenreDictionaryTable.genreId] to it[MediaGenreDictionaryTable.name]
                }
        }
    }

    private suspend fun fetchAndSaveFromTmdb(tmdbLanguage: String, targetLanguageCode: String) {
        // Качаем жанры для кино и сериалов (они частично пересекаются, но есть уникальные)
        val movieGenres = tmdbApi.getGenres(isTv = false, language = tmdbLanguage)
        val tvGenres = tmdbApi.getGenres(isTv = true, language = tmdbLanguage)

        // Объединяем и удаляем дубликаты по ID
        val combined = (movieGenres + tvGenres).distinctBy { it.id }

        if (combined.isEmpty()) {
            logger.warn("TMDB API returned empty genres for language: $tmdbLanguage")
            return
        }

        dbQuery {
            combined.forEach { genre ->
                // Проверяем существование, чтобы не кидало ошибку DuplicateKey, если вдруг что-то упало на половине транзакции
                val exists = MediaGenreDictionaryTable.selectAll().where {
                    (MediaGenreDictionaryTable.genreId eq genre.id) and (MediaGenreDictionaryTable.languageCode eq targetLanguageCode)
                }.count() > 0

                if (!exists) {
                    MediaGenreDictionaryTable.insert {
                        it[genreId] = genre.id
                        it[languageCode] = targetLanguageCode
                        it[name] = genre.name
                    }
                }
            }
        }
        logger.info("Successfully synced ${combined.size} genres for $targetLanguageCode into DB.")
    }

    private fun normalizeCode(language: String?): String {
        if (language.isNullOrBlank()) return "ru"
        val code = language.lowercase().substringBefore("-").substringBefore("_")
        return if (code in listOf("ru", "en")) code else "ru"
    }
}
