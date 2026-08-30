package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.IntegrationService

object UserExternalAuthTable : BaseUuidTable("user_external_auth") {
    val userId = uuid("user_id")
    val service = enumerationByName("service", 50, IntegrationService::class)
    val accessToken = text("access_token")
    val refreshToken = text("refresh_token").nullable()
    val expiresIn = long("expires_in").nullable()
    val settings = text("settings").nullable()
}
