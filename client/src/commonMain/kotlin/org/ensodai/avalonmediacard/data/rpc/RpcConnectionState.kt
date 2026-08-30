package org.ensodai.avalonmediacard.data.rpc

/**
 * Единые состояния жизненного цикла соединения RPC/WebSocket в Avalon Mediacard.
 */
sealed interface RpcConnectionState {
    /** Не активно (нет авторизации или сокет остановлен) */
    data object Idle : RpcConnectionState

    /** Идет процесс открытия физического WebSocket-соединения */
    data object Connecting : RpcConnectionState

    /** Физический сокет открыт, идет аутентификация через токен */
    data object Authenticating : RpcConnectionState

    /** Соединение полностью установлено, авторизовано и готово к обмену данными */
    data object Connected : RpcConnectionState

    /** Ошибка соединения */
    data class Error(val message: String) : RpcConnectionState
}
