package org.ensodai.avalonmediacard.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*
import kotlin.uuid.Uuid
import org.ensodai.avalonmediacard.contract.model.UserRole

object JwtProvider {
    private val jwtSecret: String by lazy {
        System.getenv("JWT_SECRET")?.takeIf { it.isNotBlank() } ?: "SUPER_SECRET_KEY_AVALON"
    }
    private const val ISSUER = "avalon"
    private val algorithm by lazy { Algorithm.HMAC256(jwtSecret) }

    fun generateToken(userId: Uuid, username: String, role: UserRole): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId.toString())
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) // 30 дней
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJwtPayload? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
            val decoded = verifier.verify(token)
            val userIdStr = decoded.getClaim("userId").asString() ?: return null
            val roleStr = decoded.getClaim("role").asString() ?: "USER"
            val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.USER)
            DecodedJwtPayload(
                userId = Uuid.parse(userIdStr),
                username = decoded.getClaim("username").asString() ?: return null,
                role = role
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class DecodedJwtPayload(
    val userId: Uuid,
    val username: String,
    val role: UserRole
)
