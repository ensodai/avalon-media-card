package org.ensodai.avalonmediacard.security

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Сервис криптографических Playback Tickets (AEAD AES-256-GCM).
 * Шифрует целевые медиа-ссылки и заголовки в компактные, защищенные от подделки токены URL.
 */
@Single
class StreamTokenService(
    private val systemSettingsRepository: SystemSettingsRepository
) {
    private val logger = LoggerFactory.getLogger(StreamTokenService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val gcmTagLengthBits = 128
    private val ivLengthBytes = 12
    private val tokenVersion: Byte = 0x01

    private val secretKey: SecretKey by lazy {
        initSecretKey()
    }

    private fun initSecretKey(): SecretKey {
        val envSecret = System.getenv("STREAM_PROXY_SECRET")?.takeIf { it.isNotBlank() }
        val rawKey: ByteArray = if (envSecret != null) {
            val keyBytes = envSecret.toByteArray(Charsets.UTF_8)
            if (keyBytes.size == 32) keyBytes
            else java.security.MessageDigest.getInstance("SHA-256").digest(keyBytes)
        } else {
            val stored = runCatching {
                runBlocking {
                    systemSettingsRepository.getSetting("system:stream_proxy_secret")
                }
            }.getOrNull()

            if (!stored.isNullOrBlank()) {
                Base64.getDecoder().decode(stored)
            } else {
                val generated = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                runCatching {
                    runBlocking {
                        systemSettingsRepository.saveSetting("system:stream_proxy_secret", Base64.getEncoder().encodeToString(generated))
                    }
                }
                generated
            }
        }
        return SecretKeySpec(rawKey, "AES")
    }

    data class TokenPayload(
        val targetUrl: String,
        val userId: Uuid?,
        val flags: Byte,
        val headers: Map<String, String>,
        val authHeader: String?
    )

    /**
     * Генерирует зашифрованный AES-256-GCM токен для медиа-ресурса.
     */
    fun generateToken(
        targetUrl: String,
        userId: Uuid?,
        flags: Byte = 0x00,
        headers: Map<String, String> = emptyMap(),
        authHeader: String? = null,
        ttlSeconds: Long = 900L // 15 минут по умолчанию (Sliding TTL)
    ): String {
        val expiresAt = Instant.now().epochSecond + ttlSeconds
        val urlBytes = targetUrl.toByteArray(Charsets.UTF_8)
        val headersJsonBytes = if (headers.isNotEmpty()) json.encodeToString(headers).toByteArray(Charsets.UTF_8) else ByteArray(0)
        val authBytes = authHeader?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)

        val javaUuid = userId?.toJavaUuid()
        val mostSigBits = javaUuid?.mostSignificantBits ?: 0L
        val leastSigBits = javaUuid?.leastSignificantBits ?: 0L

        // Структура:
        // Version (1) + ExpiresAt (8) + UUID (16) + Flags (1) +
        // UrlLen (2) + UrlBytes +
        // HeadersLen (2) + HeadersBytes +
        // AuthLen (2) + AuthBytes
        val payloadLength = 1 + 8 + 16 + 1 + 2 + urlBytes.size + 2 + headersJsonBytes.size + 2 + authBytes.size
        val payload = ByteBuffer.allocate(payloadLength)
            .put(tokenVersion)
            .putLong(expiresAt)
            .putLong(mostSigBits)
            .putLong(leastSigBits)
            .put(flags)
            .putShort(urlBytes.size.toShort())
            .put(urlBytes)
            .putShort(headersJsonBytes.size.toShort())
            .put(headersJsonBytes)
            .putShort(authBytes.size.toShort())
            .put(authBytes)
            .array()

        val iv = ByteArray(ivLengthBytes).apply { SecureRandom().nextBytes(this) }
        val parameterSpec = GCMParameterSpec(gcmTagLengthBits, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(payload)

        val combined = ByteBuffer.allocate(iv.size + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined)
    }

    /**
     * Расшифровывает и валидирует срок жизни токена. Возвращает null при любой ошибке или истечении срока.
     */
    fun decryptAndValidate(tokenStr: String): TokenPayload? {
        return try {
            val decoded = Base64.getUrlDecoder().decode(tokenStr)
            if (decoded.size <= ivLengthBytes + (gcmTagLengthBits / 8)) return null

            val iv = decoded.copyOfRange(0, ivLengthBytes)
            val ciphertext = decoded.copyOfRange(ivLengthBytes, decoded.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val parameterSpec = GCMParameterSpec(gcmTagLengthBits, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val payload = cipher.doFinal(ciphertext)
            val buffer = ByteBuffer.wrap(payload)

            val version = buffer.get()
            if (version != tokenVersion) return null

            val expiresAt = buffer.long
            if (Instant.now().epochSecond > expiresAt) {
                logger.debug("Playback ticket expired: now={}, expiresAt={}", Instant.now().epochSecond, expiresAt)
                return null
            }

            val mostSig = buffer.long
            val leastSig = buffer.long
            val userId = if (mostSig != 0L || leastSig != 0L) {
                java.util.UUID(mostSig, leastSig).toKotlinUuid()
            } else null

            val flags = buffer.get()

            val urlLen = buffer.short.toInt() and 0xFFFF
            val urlBytes = ByteArray(urlLen)
            buffer.get(urlBytes)
            val targetUrl = String(urlBytes, Charsets.UTF_8)

            val headersLen = buffer.short.toInt() and 0xFFFF
            val headers = if (headersLen > 0) {
                val headersBytes = ByteArray(headersLen)
                buffer.get(headersBytes)
                runCatching {
                    json.decodeFromString<Map<String, String>>(String(headersBytes, Charsets.UTF_8))
                }.getOrDefault(emptyMap())
            } else emptyMap()

            val authLen = buffer.short.toInt() and 0xFFFF
            val authHeader = if (authLen > 0) {
                val authBytes = ByteArray(authLen)
                buffer.get(authBytes)
                String(authBytes, Charsets.UTF_8)
            } else null

            TokenPayload(
                targetUrl = targetUrl,
                userId = userId,
                flags = flags,
                headers = headers,
                authHeader = authHeader
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Оборачивает URL в защищенный путь стриминг-прокси.
     */
    fun wrapUrl(
        targetUrl: String,
        userId: Uuid?,
        filename: String? = null,
        streamType: StreamType? = null,
        headers: Map<String, String> = emptyMap(),
        authHeader: String? = null,
        ttlSeconds: Long = 900L
    ): String {
        val token = generateToken(
            targetUrl = targetUrl,
            userId = userId,
            headers = headers,
            authHeader = authHeader,
            ttlSeconds = ttlSeconds
        )
        val name = filename ?: when {
            streamType == StreamType.Hls -> "playlist.m3u8"
            targetUrl.contains(".m3u8", ignoreCase = true) || targetUrl.contains("format=m3u8", ignoreCase = true) || targetUrl.contains("m3u8=", ignoreCase = true) || targetUrl.contains("/hls", ignoreCase = true) || targetUrl.contains("/balancer", ignoreCase = true) -> "playlist.m3u8"
            targetUrl.contains(".mpd", ignoreCase = true) || targetUrl.contains("/dash", ignoreCase = true) -> "manifest.mpd"
            targetUrl.contains(".ts", ignoreCase = true) || targetUrl.contains("format=ts", ignoreCase = true) -> "segment.ts"
            else -> "video.mp4"
        }
        return "/api/stream-proxy/$token/$name"
    }
}
