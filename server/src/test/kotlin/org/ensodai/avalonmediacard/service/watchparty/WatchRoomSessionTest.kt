package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class WatchRoomSessionTest {

    private fun createSession(testScope: kotlinx.coroutines.CoroutineScope): Triple<WatchRoomSession, Uuid, Uuid> {
        val roomId = Uuid.random()
        val hostId = Uuid.random()
        val guestId = Uuid.random()

        val session = WatchRoomSession(
            roomId = roomId,
            mediaId = "test_media_123",
            title = "Test Party",
            hostUserId = hostId,
            controlMode = WatchRoomControlMode.HOST_ONLY,
            initialSeason = 1,
            initialEpisode = 1,
            initialPositionSeconds = 0L,
            scope = testScope,
            onProgressChanged = { _, _, _ -> },
            onHostMigrated = { _ -> }
        )
        return Triple(session, hostId, guestId)
    }

    @Test
    fun test_initialSnapshot_on_subscribe() = runTest {
        val (session, hostId, guestId) = createSession(this)

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        val firstEvent = session.subscribeLobby().first()
        assertTrue(firstEvent is LobbyEvent.InitialSnapshot)
        assertEquals(2, firstEvent.participants.size)

        val guestDto = firstEvent.participants.find { it.userId == guestId }
        assertNotNull(guestDto)
        assertTrue(guestDto.isOnline)
        assertEquals("GuestUser", guestDto.username)
    }

    @Test
    fun test_setLobbyStatus_emits_participantUpdated() = runTest {
        val (session, hostId, guestId) = createSession(this)

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        session.setLobbyStatus(guestId, WatchParticipantIntent.BACKGROUND_LISTENING, isReady = true)
        runCurrent()

        val lastEvent = events.lastOrNull()
        assertTrue(lastEvent is LobbyEvent.ParticipantUpdated)
        assertEquals(guestId, lastEvent.participant.userId)
        assertTrue(lastEvent.participant.isReady)
        assertEquals(WatchParticipantIntent.BACKGROUND_LISTENING, lastEvent.participant.intent)

        collectJob.cancel()
    }

    @Test
    fun test_client_disconnect_marks_offline_and_keeps_in_room() = runTest {
        val (session, hostId, guestId) = createSession(this)
        val connId = Uuid.random()

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, connId)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        // Отключение клиента
        session.handleClientDisconnected(guestId, connId)
        runCurrent()

        val updateEvent = events.lastOrNull()
        assertTrue(updateEvent is LobbyEvent.ParticipantUpdated)
        assertEquals(guestId, updateEvent.participant.userId)
        assertFalse(updateEvent.participant.isOnline, "Участник должен быть помечен offline при разрыве соединения")

        // Участник остается в сессии с isOnline = false
        val participantInSession = session.getParticipantList().find { it.userId == guestId }
        assertNotNull(participantInSession, "Участник должен оставаться в комнате после отключения")
        assertFalse(participantInSession.isOnline)

        collectJob.cancel()
    }

    @Test
    fun test_reconnect_restores_online_status() = runTest {
        val (session, hostId, guestId) = createSession(this)
        val conn1 = Uuid.random()
        val conn2 = Uuid.random()

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, conn1)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        // 1. Клиент отключился
        session.handleClientDisconnected(guestId, conn1)
        runCurrent()

        // 2. Прошло 10 секунд
        advanceTimeBy(10.seconds)
        runCurrent()

        // 3. Клиент переподключился с новым соединением conn2
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, conn2)
        runCurrent()

        val lastEvent = events.lastOrNull()
        assertTrue(lastEvent is LobbyEvent.ParticipantUpdated)
        assertEquals(guestId, lastEvent.participant.userId)
        assertTrue(lastEvent.participant.isOnline, "После реконнекта участник снова должен быть online")

        val participantInSession = session.getParticipantList().find { it.userId == guestId }
        assertNotNull(participantInSession)
        assertTrue(participantInSession.isOnline)

        collectJob.cancel()
    }

    @Test
    fun test_disconnected_participant_stays_indefinitely() = runTest {
        val (session, hostId, guestId) = createSession(this)
        val conn1 = Uuid.random()

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, conn1)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        // Клиент отключился
        session.handleClientDisconnected(guestId, conn1)
        runCurrent()

        // Проматываем 60 секунд — участник должен оставаться
        advanceTimeBy(60.seconds)
        runCurrent()

        // Не должно быть события ParticipantRemoved
        val removedEvent = events.find { it is LobbyEvent.ParticipantRemoved }
        assertNull(removedEvent, "Участник НЕ должен автоматически удаляться из комнаты")

        // Участник на месте с isOnline = false
        val participantInSession = session.getParticipantList().find { it.userId == guestId }
        assertNotNull(participantInSession, "Участник должен оставаться в комнате неограниченно")
        assertFalse(participantInSession.isOnline)

        collectJob.cancel()
    }

    @Test
    fun test_overlapping_connection_teardown_does_not_mark_user_offline() = runTest {
        val (session, hostId, guestId) = createSession(this)
        val conn1 = Uuid.random()
        val conn2 = Uuid.random()

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)

        // 1. Гость подключился с conn1
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, conn1)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        // 2. Гость открыл вторую вкладку или быстро переподключился (conn2) ДО того, как conn1 закрылся
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER, conn2)
        runCurrent()

        // 3. Старое соединение conn1 наконец завершается
        session.handleClientDisconnected(guestId, conn1)
        runCurrent()

        // Проверяем: гость НЕ должен стать offline, потому что conn2 все еще активен!
        val participantInSession = session.getParticipantList().find { it.userId == guestId }
        assertNotNull(participantInSession)
        assertTrue(participantInSession.isOnline, "Участник должен оставаться online, пока активно хотя бы одно соединение")

        // В поток не должно было прилететь событие offline
        val hasOfflineEvent = events.any { it is LobbyEvent.ParticipantUpdated && !it.participant.isOnline }
        assertFalse(hasOfflineEvent, "Не должно рассылаться событие offline при наличии другого активного соединения")

        collectJob.cancel()
    }

    @Test
    fun test_triggerStartPlayback_permissions_and_transition() = runTest {
        val (session, hostId, guestId) = createSession(this)

        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        val events = mutableListOf<LobbyEvent>()
        val collectJob = launch {
            session.lobbyEvents.collect { events.add(it) }
        }
        runCurrent()

        // 1. Гость пытается запустить воспроизведение в HOST_ONLY комнате
        val guestAttempt = session.triggerStartPlayback(guestId)
        assertFalse(guestAttempt, "Гость не имеет права запускать воспроизведение в режиме HOST_ONLY")

        // 2. Хост запускает воспроизведение
        val hostAttempt = session.triggerStartPlayback(hostId)
        assertTrue(hostAttempt, "Хост должен успешно запустить воспроизведение")
        runCurrent()

        val transition = events.find { it is LobbyEvent.TransitionToPlayer } as? LobbyEvent.TransitionToPlayer
        assertNotNull(transition, "Сессия должна отправить событие TransitionToPlayer")
        assertEquals(1, transition.season)
        assertEquals(1, transition.episode)
        assertTrue(transition.playAtServerTimestampMs > 0)

        collectJob.cancel()
    }
}
