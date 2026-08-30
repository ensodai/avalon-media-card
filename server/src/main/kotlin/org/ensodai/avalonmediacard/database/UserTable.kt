package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus

object UserTable : BaseUuidTable("users") {
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.USER)
    val status = enumerationByName("status", 20, UserStatus::class).default(UserStatus.ACTIVE)
}
