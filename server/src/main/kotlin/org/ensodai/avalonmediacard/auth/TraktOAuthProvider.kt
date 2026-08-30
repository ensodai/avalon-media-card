package org.ensodai.avalonmediacard.auth

import app.moviebase.trakt.Trakt
import app.moviebase.trakt.model.TraktUserSlug
import io.ktor.client.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.utils.EnvHelper
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory

@Single
class TraktOAuthProvider(
    private val client: HttpClient,
    private val userExternalAuthRepository: UserExternalAuthRepository
) : OAuthProvider {
    private val logger = LoggerFactory.getLogger(TraktOAuthProvider::class.java)

    override val serviceName: String = "trakt"
    override val displayName: String = "Trakt.tv"
    override val description: String =
        "Синхронизируйте историю просмотров, прогресс воспроизведения и ваши оценки фильмов и сериалов."

    private val clientId: String
        get() = EnvHelper.getEnv("TRAKT_CLIENT_ID") ?: ""

    private val clientSecret: String
        get() = EnvHelper.getEnv("TRAKT_CLIENT_SECRET") ?: ""

    private val redirectUri: String
        get() = EnvHelper.getEnv("TRAKT_REDIRECT_URI") ?: "http://localhost:8081"

    override fun getAuthUrl(state: String): String {
        return "https://trakt.tv/oauth/authorize?response_type=code&client_id=$clientId&redirect_uri=$redirectUri&state=$state"
    }

    override suspend fun exchangeCode(code: String): TokenResponse {
        val trakt = Trakt {
            clientId = this@TraktOAuthProvider.clientId
            clientSecret = this@TraktOAuthProvider.clientSecret
        }
        return try {
            val response = trakt.auth.requestAccessToken(redirectUri, code)
            TokenResponse(
                accessToken = response.accessToken ?: throw Exception("AccessToken is null in response"),
                refreshToken = response.refreshToken ?: "",
                expiresIn = response.expiresIn?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            logger.error("Failed to exchange code", e)
            throw Exception("Failed to exchange Trakt OAuth code", e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): TokenResponse {
        val trakt = Trakt {
            clientId = this@TraktOAuthProvider.clientId
            clientSecret = this@TraktOAuthProvider.clientSecret
        }
        return try {
            val response = trakt.auth.requestRefreshToken(redirectUri, refreshToken)
            TokenResponse(
                accessToken = response.accessToken ?: throw Exception("AccessToken is null in response"),
                refreshToken = response.refreshToken ?: "",
                expiresIn = response.expiresIn?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            logger.error("Failed to refresh token", e)
            throw Exception("Failed to refresh Trakt token", e)
        }
    }

    override suspend fun getUserProfile(accessToken: String): String? {
        val trakt = Trakt {
            clientId = this@TraktOAuthProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }
        return try {
            val profile = trakt.users.getProfile(TraktUserSlug.ME)
            profile.userName
        } catch (e: Exception) {
            logger.error("Failed to fetch user profile", e)
            null
        }
    }

    override suspend fun revokeToken(accessToken: String): Boolean {
        return try {
            val response = client.post("https://api.trakt.tv/oauth/revoke") {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "token" to accessToken,
                        "client_id" to clientId,
                        "client_secret" to clientSecret
                    )
                )
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            logger.error("Failed to revoke Trakt token", e)
            false
        }
    }
}
