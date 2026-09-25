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
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
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
        assertTrue(transition.playAtServerTime.toEpochMilliseconds() > 0)

        collectJob.cancel()
    }

    @Test
    fun test_syncState_uses_instant_and_computes_virtual_position() = runTest {
        val (session, hostId, _) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)

        session.handleCommand(hostId, org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand.Play(positionMs = 10_000L))
        runCurrent()

        val syncState = session.getCurrentSyncState()
        assertTrue(syncState.isPlaying)
        assertEquals(10_000L, syncState.anchorPositionMs)
        assertTrue(syncState.anchorServerTime.toEpochMilliseconds() > 0)
    }

    @Test
    fun test_preroll_buffering_does_not_pause_room_and_starts_playing_if_ready() = runTest {
        val (session, hostId, guestId) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        // Хост запускает воспроизведение
        session.handleCommand(hostId, RoomPlaybackCommand.Play(positionMs = 10_000L))
        runCurrent()
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
        assertTrue(session.isPlaying)

        // Гость рапортует буферизацию во время преролла (загрузка чанков)
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = true))
        runCurrent()
        advanceTimeBy(500.milliseconds)
        runCurrent()

        // Во время преролла комната НЕ должна вставать на паузу!
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
        assertTrue(session.isPlaying, "Буферизация во время STARTING_SCHEDULED не должна ставить комнату на паузу")

        // Гость закончил загрузку чанков до момента старта
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = false))
        runCurrent()

        // Наступает T_start (+1500мс от старта)
        advanceTimeBy(1100.milliseconds)
        runCurrent()

        // Комната должна перейти в активное воспроизведение PLAYING_IN_SYNC
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)
        assertTrue(session.isPlaying)
    }

    @Test
    fun test_preroll_participant_still_buffering_at_t_start_triggers_partial_buffering_pause() = runTest {
        val (session, hostId, guestId) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        // Хост запускает воспроизведение
        session.handleCommand(hostId, RoomPlaybackCommand.Play(positionMs = 10_000L))
        runCurrent()
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)

        // Гость рапортует буферизацию
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = true))
        runCurrent()

        // Наступает момент T_start (+1500мс), но гость ВСЕ ЕЩЕ буферизуется
        advanceTimeBy(1600.milliseconds)
        runCurrent()

        // Комната должна перейти в PARTIAL_BUFFERING и встать на паузу
        assertEquals(RoomPhase.PARTIAL_BUFFERING, session.phase)
        assertFalse(session.isPlaying, "Если участник не готов к T_start, комната должна встать на паузу")
        assertEquals(10_000L, session.anchorPositionMs)

        // Гость наконец завершил буферизацию
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = false))
        runCurrent()

        // Комната переходит в STARTING_SCHEDULED с коротким lead time 500мс
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
        assertTrue(session.isPlaying)

        // Спустя 500мс комната начинает играть
        advanceTimeBy(600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)
    }

    @Test
    fun test_live_buffering_in_playing_in_sync_pauses_room_into_partial_buffering() = runTest {
        val (session, hostId, guestId) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        // Старт и переход в PLAYING_IN_SYNC
        session.handleCommand(hostId, RoomPlaybackCommand.Play(positionMs = 10_000L))
        runCurrent()
        advanceTimeBy(1600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)

        // Гость словил аварийную буферизацию посреди просмотра
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = true))
        runCurrent()

        // Комната должна немедленно перейти в PARTIAL_BUFFERING
        assertEquals(RoomPhase.PARTIAL_BUFFERING, session.phase)
        assertFalse(session.isPlaying, "Аварийная буферизация в PLAYING_IN_SYNC должна перевести комнату в PARTIAL_BUFFERING")

        // Гость восстановил соединение
        session.handleCommand(guestId, RoomPlaybackCommand.ReportBuffer(isBuffering = false))
        runCurrent()

        // Возобновление с lead time 500мс
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
        advanceTimeBy(600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)
    }

    @Test
    fun test_seek_while_playing_sets_2000ms_lead_time() = runTest {
        val (session, hostId, _) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)

        // Старт воспроизведения
        session.handleCommand(hostId, RoomPlaybackCommand.Play(positionMs = 10_000L))
        runCurrent()
        assertTrue(session.isPlaying)

        // Хост делает Seek на 45_000 мс во время воспроизведения
        val beforeSeek = Clock.System.now()
        session.handleCommand(hostId, RoomPlaybackCommand.Seek(targetPositionMs = 45_000L))
        runCurrent()

        val syncState = session.getCurrentSyncState()
        assertEquals(45_000L, syncState.anchorPositionMs)
        // Lead Time должен быть около 2000 мс в будущем
        val leadTimeMs = (syncState.anchorServerTime - beforeSeek).inWholeMilliseconds
        assertTrue(leadTimeMs in 1800..2500, "Lead time при Seek во время воспроизведения должен быть ~2000 мс (было: $leadTimeMs ms)")
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
    }

    @Test
    fun test_triggerStartPlayback_enters_preparing_and_waits_for_all_participants() = runTest {
        val (session, hostId, guestId) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        // Хост запускает воспроизведение из лобби
        session.triggerStartPlayback(hostId)
        runCurrent()

        // Комната переходит в PREPARING
        assertEquals(RoomPhase.PREPARING, session.phase)
        assertFalse(session.isPlaying)
        assertEquals(0L, session.anchorPositionMs)

        // Прошло 5 секунд (Chrome уже готов за 4.7с)
        advanceTimeBy(5.seconds)
        runCurrent()
        session.handleCommand(hostId, RoomPlaybackCommand.ReportMediaReady(positionMs = 0L))
        runCurrent()

        // Firefox еще загружает видео (14.8с) -> комната все еще в PREPARING и на паузе!
        assertEquals(RoomPhase.PREPARING, session.phase)
        assertFalse(session.isPlaying, "Комната должна оставаться в PREPARING, пока второй участник не загрузит видео")

        // Прошло еще 10 секунд (всего 15 секунд), 8-секундный таймер НЕ должен был сработать
        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(RoomPhase.PREPARING, session.phase)
        assertFalse(session.isPlaying)

        // Firefox наконец закончил демультиплексирование и прислал ReportMediaReady
        session.handleCommand(guestId, RoomPlaybackCommand.ReportMediaReady(positionMs = 0L))
        runCurrent()

        // Теперь оба готовы! Комната переходит в STARTING_SCHEDULED (+1500 мс)
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)
        assertTrue(session.isPlaying)

        // Наступает T_start (+1500мс)
        advanceTimeBy(1600.milliseconds)
        runCurrent()

        // Оба синхронно начинают просмотр с 0с!
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)
        assertTrue(session.isPlaying)
    }

    @Test
    fun test_preparation_timeout_marks_unresponsive_participant_desynced() = runTest {
        val (session, hostId, guestId) = createSession(this)
        session.handleClientConnected(hostId, "HostUser", WatchRoomParticipantRole.HOST)
        session.handleClientConnected(guestId, "GuestUser", WatchRoomParticipantRole.MEMBER)

        // Хост запускает воспроизведение
        session.triggerStartPlayback(hostId)
        runCurrent()

        assertEquals(RoomPhase.PREPARING, session.phase)

        // Хост готов
        session.handleCommand(hostId, RoomPlaybackCommand.ReportMediaReady(positionMs = 0L))
        runCurrent()

        // Гость завис или ушел AFK на 60 секунд
        advanceTimeBy(61.seconds)
        runCurrent()

        // Комната должна была пометить гостя desynced и перейти в STARTING_SCHEDULED
        val guest = session.getParticipantList().find { it.userId == guestId }
        assertNotNull(guest)
        assertEquals(RoomPhase.STARTING_SCHEDULED, session.phase)

        // Спустя 1500мс начинается воспроизведение для хоста
        advanceTimeBy(1600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)
    }
}
