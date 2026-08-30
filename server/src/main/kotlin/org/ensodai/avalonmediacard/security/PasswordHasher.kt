package org.ensodai.avalonmediacard.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val hash = factory.generateSecret(spec).encoded
        val encoder = Base64.getEncoder()
        return "pbkdf2\$${ITERATIONS}\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(hash)}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        return try {
            if (storedHash.startsWith("pbkdf2$")) {
                val parts = storedHash.split("$")
                if (parts.size != 4) return false
                val iterations = parts[1].toInt()
                val salt = Base64.getDecoder().decode(parts[2])
                val expectedHash = Base64.getDecoder().decode(parts[3])
                val spec = PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.size * 8)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
                val actualHash = factory.generateSecret(spec).encoded
                MessageDigest.isEqual(actualHash, expectedHash)
            } else {
                // Fallback для легаси формата SHA-256 с солью (salt:hash)
                val parts = storedHash.split(":")
                if (parts.size != 2) return false
                val salt = Base64.getDecoder().decode(parts[0])
                val expectedHash = Base64.getDecoder().decode(parts[1])
                val md = MessageDigest.getInstance("SHA-256")
                md.update(salt)
                val actualHash = md.digest(password.toByteArray(Charsets.UTF_8))
                MessageDigest.isEqual(actualHash, expectedHash)
            }
        } catch (_: Exception) {
            false
        }
    }
}
