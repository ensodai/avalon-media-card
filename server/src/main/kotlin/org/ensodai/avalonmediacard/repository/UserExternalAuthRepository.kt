package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.database.UserExternalAuthTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class UserExternalAuth(
    val id: Uuid,
    val userId: Uuid,
    val service: IntegrationService,
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long?,
    val settings: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Single
class UserExternalAuthRepository {

    suspend fun saveToken(
        userId: Uuid,
        service: IntegrationService,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long?
    ): Boolean = dbQuery {
        try {
            val exists = UserExternalAuthTable.selectAll()
                .where { (UserExternalAuthTable.userId eq userId) and (UserExternalAuthTable.service eq service) }
                .any()

            if (exists) {
                UserExternalAuthTable.update({
                    (UserExternalAuthTable.userId eq userId) and (UserExternalAuthTable.service eq service)
                }) {
                    it[UserExternalAuthTable.accessToken] = accessToken
                    it[UserExternalAuthTable.refreshToken] = refreshToken
                    it[UserExternalAuthTable.expiresIn] = expiresIn
                }
            } else {
                UserExternalAuthTable.insert {
                    it[id] = Uuid.random()
                    it[UserExternalAuthTable.userId] = userId
                    it[UserExternalAuthTable.service] = service
                    it[UserExternalAuthTable.accessToken] = accessToken
                    it[UserExternalAuthTable.refreshToken] = refreshToken
                    it[UserExternalAuthTable.expiresIn] = expiresIn
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getToken(userId: Uuid, service: IntegrationService): UserExternalAuth? = dbQuery {
        UserExternalAuthTable.selectAll()
            .where { (UserExternalAuthTable.userId eq userId) and (UserExternalAuthTable.service eq service) }
            .map {
                UserExternalAuth(
                    id = it[UserExternalAuthTable.id].value,
                    userId = it[UserExternalAuthTable.userId],
                    service = it[UserExternalAuthTable.service],
                    accessToken = it[UserExternalAuthTable.accessToken],
                    refreshToken = it[UserExternalAuthTable.refreshToken],
                    expiresIn = it[UserExternalAuthTable.expiresIn],
                    settings = it[UserExternalAuthTable.settings],
                    createdAt = it[UserExternalAuthTable.createdAt],
                    updatedAt = it[UserExternalAuthTable.updatedAt]
                )
            }
            .firstOrNull()
    }

    suspend fun deleteToken(userId: Uuid, service: IntegrationService): Boolean = dbQuery {
        try {
            val deleted = UserExternalAuthTable.deleteWhere {
                (UserExternalAuthTable.userId eq userId) and (UserExternalAuthTable.service eq service)
            }
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getTokensForService(service: IntegrationService): List<UserExternalAuth> = dbQuery {
        UserExternalAuthTable.selectAll()
            .where { UserExternalAuthTable.service eq service }
            .map {
                UserExternalAuth(
                    id = it[UserExternalAuthTable.id].value,
                    userId = it[UserExternalAuthTable.userId],
                    service = it[UserExternalAuthTable.service],
                    accessToken = it[UserExternalAuthTable.accessToken],
                    refreshToken = it[UserExternalAuthTable.refreshToken],
                    expiresIn = it[UserExternalAuthTable.expiresIn],
                    settings = it[UserExternalAuthTable.settings],
                    createdAt = it[UserExternalAuthTable.createdAt],
                    updatedAt = it[UserExternalAuthTable.updatedAt]
                )
            }
    }

    suspend fun updateSettings(userId: Uuid, service: IntegrationService, settingsJson: String): Boolean = dbQuery {
        try {
            UserExternalAuthTable.update({
                (UserExternalAuthTable.userId eq userId) and (UserExternalAuthTable.service eq service)
            }) {
                it[UserExternalAuthTable.settings] = settingsJson
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
