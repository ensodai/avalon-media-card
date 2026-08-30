package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.UserInfo
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.database.UserTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.ensodai.avalonmediacard.security.PasswordHasher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class UserRepository {

    suspend fun createUser(username: String, passwordHash: String, role: UserRole = UserRole.USER, status: org.ensodai.avalonmediacard.contract.model.UserStatus = org.ensodai.avalonmediacard.contract.model.UserStatus.ACTIVE): Uuid = dbQuery {
        val newId = Uuid.random()
        UserTable.insert {
            it[id] = newId
            it[UserTable.username] = username
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.role] = role
            it[UserTable.status] = status
        }
        newId
    }

    suspend fun findByUsername(username: String): UserInfo? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .map {
                UserInfo(
                    id = it[UserTable.id].value,
                    username = it[UserTable.username],
                    role = it[UserTable.role],
                    status = it[UserTable.status]
                )
            }
            .firstOrNull()
    }

    suspend fun getAllUsers(): List<UserInfo> = dbQuery {
        UserTable.selectAll()
            .map {
                UserInfo(
                    id = it[UserTable.id].value,
                    username = it[UserTable.username],
                    role = it[UserTable.role],
                    status = it[UserTable.status]
                )
            }
    }

    suspend fun findById(id: Uuid): UserInfo? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.id eq id }
            .map {
                UserInfo(
                    id = it[UserTable.id].value,
                    username = it[UserTable.username],
                    role = it[UserTable.role],
                    status = it[UserTable.status]
                )
            }
            .firstOrNull()
    }

    suspend fun getPasswordHashByUsername(username: String): String? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .map { it[UserTable.passwordHash] }
            .firstOrNull()
    }

    suspend fun updatePasswordHash(username: String, newPasswordHash: String): Unit = dbQuery {
        UserTable.update({ UserTable.username eq username }) {
            it[passwordHash] = newPasswordHash
        }
    }

    suspend fun updatePasswordHashById(id: Uuid, newPasswordHash: String): Boolean = dbQuery {
        val updatedRows = UserTable.update({ UserTable.id eq id }) {
            it[passwordHash] = newPasswordHash
        }
        updatedRows > 0
    }

    suspend fun updateStatus(id: Uuid, newStatus: org.ensodai.avalonmediacard.contract.model.UserStatus): Boolean = dbQuery {
        val updatedRows = UserTable.update({ UserTable.id eq id }) {
            it[status] = newStatus
        }
        updatedRows > 0
    }

    suspend fun createAdminIfNotExists(username: String, passwordRaw: String) {
        val existing = findByUsername(username)
        if (existing == null) {
            val passwordHash = PasswordHasher.hash(passwordRaw)
            createUser(username, passwordHash, UserRole.ADMIN)
            println("Администратор $username успешно создан.")
        } else {
            val currentHash = getPasswordHashByUsername(username)
            if (currentHash != null && !PasswordHasher.verify(passwordRaw, currentHash)) {
                val newHash = PasswordHasher.hash(passwordRaw)
                updatePasswordHash(username, newHash)
                println("Пароль администратора $username успешно обновлен.")
            }
            if (existing.role != UserRole.ADMIN) {
                updateRole(existing.id, UserRole.ADMIN)
                println("Роль пользователя $username обновлена до ADMIN.")
            }
        }
    }

    suspend fun updateRole(id: Uuid, newRole: UserRole): Boolean = dbQuery {
        val updatedRows = UserTable.update({ UserTable.id eq id }) {
            it[role] = newRole
        }
        updatedRows > 0
    }

    suspend fun deleteUser(id: Uuid): Boolean = dbQuery {
        val deletedRows = UserTable.deleteWhere { UserTable.id eq id }
        deletedRows > 0
    }

    suspend fun countTotalUsers(): Long = dbQuery {
        UserTable.selectAll().count()
    }

    suspend fun countActiveUsers(): Long = dbQuery {
        UserTable.selectAll().where { UserTable.status eq org.ensodai.avalonmediacard.contract.model.UserStatus.ACTIVE }.count()
    }
}
