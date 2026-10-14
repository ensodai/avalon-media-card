package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import kotlin.uuid.Uuid

enum class RoomMode {
    SOLO,
    PARTY
}

/**
 * Стратегия поведения комнаты / сессии воспроизведения.
 * Позволяет подменять логику синхронизации, команд и рассылки событий:
 * - SoloRoomStrategy: для одиночного просмотра (нулевой спам эвентов, независимый плеер).
 * - PartyRoomStrategy: для совместного просмотра (TrueSync FSM, барьеры готовности, чат).
 */
interface RoomBehaviorStrategy {
    val mode: RoomMode

    /**
     * Создание потока событий для подключенного клиента.
     */
    fun createEventsFlow(
        session: WatchRoomSession,
        user: AuthState.Authorized,
        connectionId: Uuid
    ): Flow<WatchRoomEvent>

    /**
     * Обработка команд воспроизведения.
     */
    suspend fun handleCommand(
        session: WatchRoomSession,
        userId: Uuid,
        command: RoomPlaybackCommand
    ): Boolean

    /**
     * Реакция на изменение состава или статуса участников (подключение, отключение, смена готовности).
     */
    suspend fun onParticipantStateChanged(session: WatchRoomSession) {}

    /**
     * Реакция на отключение всех участников из комнаты.
     */
    suspend fun onAllParticipantsDisconnected(session: WatchRoomSession) {}

    /**
     * Очистка внутренних таймеров и корутин стратегии.
     */
    fun close() {}
}
