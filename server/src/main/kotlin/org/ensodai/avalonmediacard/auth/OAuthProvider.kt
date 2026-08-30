package org.ensodai.avalonmediacard.auth

interface OAuthProvider {
    val serviceName: String
    val displayName: String
    val description: String
    val comingSoon: Boolean get() = false

    fun getAuthUrl(state: String): String
    suspend fun exchangeCode(code: String): TokenResponse
    suspend fun refreshToken(refreshToken: String): TokenResponse
    suspend fun getUserProfile(accessToken: String): String?
    suspend fun revokeToken(accessToken: String): Boolean = false
}
