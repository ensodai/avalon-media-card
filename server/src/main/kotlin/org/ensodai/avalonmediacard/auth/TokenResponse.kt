package org.ensodai.avalonmediacard.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long?
)
