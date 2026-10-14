package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * Стратегия для одиночного просмотра (SOLO).
 * - Полностью изолирована от логики TrueSync (не рассылает SyncState, прероллы и барьеры готовности).
 * - Клиентский плеер воспроизводит видео независимо в своем темпе.
 * - Присутствие клиента в сессии служит триггером для подогрева TorrServer на сервере.
 */
class SoloRoomStrategy : RoomBehaviorStrategy {
    private val logger = LoggerFactory.getLogger(SoloRoomStrategy::class.java)

    override val mode: RoomMode
        get() = RoomMode.SOLO

    override fun createEventsFlow(
        session: WatchRoomSession,
        user: AuthState.Authorized,
        connectionId: Uuid
    ): Flow<WatchRoomEvent> = flow {
        logger.debug("[SoloRoomStrategy] Client {} connected to solo session {}", user.username, session.roomId)
        emit(WatchRoomEvent.SystemNotice("Solo session active"))
        val heartbeats = flow {
            while (currentCoroutineContext().isActive) {
                delay(20_000L)
                emit(WatchRoomEvent.SystemNotice("keepalive"))
            }
        }
        emitAll(merge(session.events, heartbeats))
    }

    override suspend fun handleCommand(
        session: WatchRoomSession,
        userId: Uuid,
        command: RoomPlaybackCommand
    ): Boolean {
        logger.debug("[SoloRoomStrategy] Received command {} from user {} in solo room {}", command, userId, session.roomId)
        return true
    }

    override suspend fun onParticipantStateChanged(session: WatchRoomSession) {
        // No-op for solo room
    }

    override suspend fun onAllParticipantsDisconnected(session: WatchRoomSession) {
        // No-op for solo room
    }

    override fun close() {
        // No-op for solo room
    }
}
