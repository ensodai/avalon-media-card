package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

open class RpcCallExecutor(
    private val manager: RpcConnectionManager
) {
    private val logger = AppLogging.logger("RpcCallExecutor")

    /**
     * Выполняет RPC-вызов с автоматическим перехватом сетевых обрывов (Doze mode, Client cancelled,
     * Broken pipe, Connection reset) и прозрачным повтором на свежем сокете.
     *
     * @param method Имя RPC-метода для логов
     * @param maxAttempts Максимальное количество попыток (по умолчанию 3)
     * @param getService Лямбда получения сгенерированного @Rpc сервиса
     * @param block Выполняемый RPC-вызов
     */
    suspend fun <S : Any, R> execute(
        method: String,
        maxAttempts: Int = 3,
        getService: suspend () -> S,
        block: suspend S.() -> R
    ): R {
        var attempt = 0
        var currentBackoff = 300L
        val maxBackoff = 5000L
        val start = Clock.System.now()

        while (currentCoroutineContext().isActive) {
            attempt++
            logger.d { "--> $method (attempt $attempt/$maxAttempts)" }
            try {
                val service = getService()
                val result = service.block()
                val durationMs = (Clock.System.now() - start).inWholeMilliseconds
                logger.d { "<-- $method [${durationMs}ms] (attempt $attempt)" }
                return result
            } catch (e: Throwable) {
                // Если отмена вызвана закрытием экрана/скоупа корутины (не сетевой сбой) - пробрасываем выше
                if (e is CancellationException) {
                    if (!currentCoroutineContext().isActive || !isNetworkCancellation(e)) {
                        val durationMs = (Clock.System.now() - start).inWholeMilliseconds
                        logger.d { "<-- $method CANCELLED by caller scope [${durationMs}ms]" }
                        throw e
                    }
                }

                val isNetworkError = isNetworkCancellation(e)
                val isTerminal = isTerminalError(e)

                if (!isNetworkError || isTerminal || attempt >= maxAttempts) {
                    val durationMs = (Clock.System.now() - start).inWholeMilliseconds
                    logger.e(e) { "<-- $method FAILED permanently [${durationMs}ms]: ${e.message}" }
                    throw e
                }

                logger.w(e) { "⚠️ Network drop during $method (attempt $attempt/$maxAttempts, error: ${e.message}). Invalidating dead connection and retrying..." }
                manager.invalidate()

                // Экспоненциальный backoff с Jitter
                val jitter = Random.nextLong(0, 100)
                delay((currentBackoff + jitter).coerceAtMost(maxBackoff).milliseconds)
                currentBackoff = (currentBackoff * 2).coerceAtMost(maxBackoff)
            }
        }

        throw CancellationException("RpcCallExecutor scope was cancelled during $method")
    }

    /**
     * Распознавание сетевых сбоев сокета (в отличие от намеренной отмены пользовательского интерфейса).
     */
    open fun isNetworkCancellation(e: Throwable): Boolean {
        if (e is CancellationException) {
            val message = e.message.orEmpty()
            if (message.contains("Client cancelled", ignoreCase = true) ||
                message.contains("RpcClient was cancelled", ignoreCase = true) ||
                message.contains("Parent job is", ignoreCase = true)
            ) {
                return true
            }
        }
        val message = e.message.orEmpty()
        val className = e::class.simpleName.orEmpty()
        return message.contains("Client cancelled", ignoreCase = true) ||
            message.contains("RpcClient was cancelled", ignoreCase = true) ||
            message.contains("ClosedReceiveChannelException", ignoreCase = true) ||
            message.contains("Broken pipe", ignoreCase = true) ||
            message.contains("Connection reset", ignoreCase = true) ||
            message.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("ConnectException", ignoreCase = true) ||
            className.contains("ClosedReceiveChannelException", ignoreCase = true) ||
            className.contains("SocketTimeoutException", ignoreCase = true) ||
            className.contains("IOException", ignoreCase = true)
    }

    /**
     * Проверка на терминальные ошибки, которые нет смысла повторять.
     */
    open fun isTerminalError(e: Throwable): Boolean {
        val message = e.message.orEmpty()
        return message.contains("400 Bad Request", ignoreCase = true) ||
            message.contains("403 Forbidden", ignoreCase = true) ||
            message.contains("404 Not Found", ignoreCase = true)
    }
}
