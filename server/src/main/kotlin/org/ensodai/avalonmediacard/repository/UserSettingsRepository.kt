package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.database.UserSettingsTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class UserSettingsRepository {

    suspend fun getUserSettings(userId: Uuid): UserSettingsDto {
        return dbQuery {
            val row = UserSettingsTable
                .selectAll()
                .where { UserSettingsTable.userId eq userId }
                .singleOrNull()

            if (row != null) {
                val locale = row[UserSettingsTable.locale]
                val posterLang = row[UserSettingsTable.posterLanguage]
                val titleModeStr = row[UserSettingsTable.titleMode]
                val overviewLang = row[UserSettingsTable.overviewLanguage]

                val titleLanguage = when (titleModeStr) {
                    "LOCALIZED", "auto" -> null
                    "ORIGINAL" -> "original"
                    else -> titleModeStr
                }
                val titleMode = if (titleLanguage == "original" || titleModeStr == "ORIGINAL") {
                    TitleDisplayMode.ORIGINAL
                } else {
                    TitleDisplayMode.LOCALIZED
                }
                val tmdbToken = row[UserSettingsTable.tmdbReadToken]

                UserSettingsDto(
                    uiLocale = locale,
                    posterLanguage = posterLang,
                    titleMode = titleMode,
                    titleLanguage = titleLanguage,
                    overviewLanguage = overviewLang,
                    tmdbReadToken = tmdbToken
                )
            } else {
                UserSettingsDto()
            }
        }
    }

    suspend fun saveUserSettings(userId: Uuid, settings: UserSettingsDto) {
        val titleModeVal = settings.titleLanguage ?: settings.titleMode.name
        dbQuery {
            val exists = UserSettingsTable
                .selectAll()
                .where { UserSettingsTable.userId eq userId }
                .empty().not()

            if (exists) {
                UserSettingsTable.update({ UserSettingsTable.userId eq userId }) {
                    it[UserSettingsTable.locale] = settings.uiLocale
                    it[UserSettingsTable.posterLanguage] = settings.posterLanguage
                    it[UserSettingsTable.titleMode] = titleModeVal
                    it[UserSettingsTable.overviewLanguage] = settings.overviewLanguage
                    it[UserSettingsTable.tmdbReadToken] = settings.tmdbReadToken?.trim()
                }
            } else {
                UserSettingsTable.insert {
                    it[UserSettingsTable.userId] = userId
                    it[UserSettingsTable.locale] = settings.uiLocale
                    it[UserSettingsTable.posterLanguage] = settings.posterLanguage
                    it[UserSettingsTable.titleMode] = titleModeVal
                    it[UserSettingsTable.overviewLanguage] = settings.overviewLanguage
                    it[UserSettingsTable.tmdbReadToken] = settings.tmdbReadToken?.trim()
                }
            }
        }
    }

    suspend fun getUserLocale(userId: Uuid): String {
        return dbQuery {
            UserSettingsTable
                .selectAll()
                .where { UserSettingsTable.userId eq userId }
                .map { it[UserSettingsTable.locale] }
                .singleOrNull() ?: "ru"
        }
    }

    suspend fun saveUserLocale(userId: Uuid, locale: String) {
        dbQuery {
            val exists = UserSettingsTable
                .selectAll()
                .where { UserSettingsTable.userId eq userId }
                .empty().not()

            if (exists) {
                UserSettingsTable.update({ UserSettingsTable.userId eq userId }) {
                    it[UserSettingsTable.locale] = locale
                }
            } else {
                UserSettingsTable.insert {
                    it[UserSettingsTable.userId] = userId
                    it[UserSettingsTable.locale] = locale
                }
            }
        }
    }
}
