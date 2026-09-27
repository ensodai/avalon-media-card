package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.repository.UserEpisodeRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class WatchPartyBrutalLifecycleStressTest {

    private class BrutalFakeWatchRoomRepository(
        val rooms: MutableList<WatchRoomSummaryDto> = mutableListOf()
    ) : WatchRoomRepository {
        var lastSavedSeason: Int? = null
        var lastSavedEpisode: Int? = null
        var lastSavedPositionSec: Long = -1L
        var lastSavedStatus: WatchRoomStatus? = null

        override suspend fun createRoom(hostUserId: Uuid, request: CreateRoomRequest, pin: String): WatchRoomDto = TODO()
        override suspend fun findRoomById(roomId: Uuid): WatchRoomDto? {
            val r = rooms.find { it.id == roomId } ?: return null
            return WatchRoomDto(
                id = r.id,
                title = r.title,
                mediaId = r.mediaId,
                mediaType = r.mediaType,
                currentSeason = r.currentSeason,
                currentEpisode = r.currentEpisode,
                lastPositionSeconds = r.lastPositionSeconds,
                hostUserId = r.participants.firstOrNull { it.role == WatchRoomParticipantRole.HOST }?.userId ?: Uuid.random(),
                controlMode = WatchRoomControlMode.HOST_ONLY,
                status = r.status,
                joinPin = r.joinPin,
                participants = r.participants
            )
        }
        override suspend fun findRoomByPin(pin: String): WatchRoomDto? = TODO()
        override suspend fun getRoomsForMedia(mediaId: String, currentUserId: Uuid, limit: Int, offset: Long): List<WatchRoomSummaryDto> =
            rooms.filter { it.mediaId == mediaId && it.status != WatchRoomStatus.ARCHIVED }
        override suspend fun getRoomsForUser(userId: Uuid, limit: Int, offset: Long): List<WatchRoomSummaryDto> = rooms
        override suspend fun updateRoomProgress(roomId: Uuid, season: Int?, episode: Int?, positionSeconds: Long) {
            lastSavedSeason = season
            lastSavedEpisode = episode
            lastSavedPositionSec = positionSeconds
            val idx = rooms.indexOfFirst { it.id == roomId }
            if (idx != -1) {
                rooms[idx] = rooms[idx].copy(
                    currentSeason = season,
                    currentEpisode = episode,
                    lastPositionSeconds = positionSeconds
                )
            }
        }
        override suspend fun updateRoomStatus(roomId: Uuid, status: WatchRoomStatus) {
            lastSavedStatus = status
            val idx = rooms.indexOfFirst { it.id == roomId }
            if (idx != -1) {
                rooms[idx] = rooms[idx].copy(status = status)
            }
        }
        override suspend fun updateRoomControlMode(roomId: Uuid, controlMode: WatchRoomControlMode) {}
        override suspend fun addParticipant(roomId: Uuid, userId: Uuid, role: WatchRoomParticipantRole): Boolean = true
        override suspend fun removeParticipant(roomId: Uuid, userId: Uuid): Boolean = true
        override suspend fun getParticipants(roomId: Uuid): List<WatchRoomParticipantDto> = emptyList()
        override suspend fun transferHost(roomId: Uuid, newHostUserId: Uuid): Boolean = true
        override suspend fun deleteRoom(roomId: Uuid): Boolean = true
    }

    private fun createAuthUser(name: String, id: Uuid = Uuid.random()): AuthState.Authorized =
        AuthState.Authorized(userId = id, username = name, role = UserRole.USER)

    /**
     * Сценарий 1: Ловим и доказываем баг с «зомби-сокетом» лобби и зависанием «Идет просмотр».
     *
     * 1. Хост и Гость подключаются к лобби (streamLobbyState).
     * 2. Хост запускает просмотр -> оба переходят в плеер (streamEvents).
     * 3. Воспроизведение идет до 150 секунд.
     * 4. Если фронтенд закрывает streamEvents, но оставляет открытым streamLobbyState:
     *    Сервер ДОЛЖЕН видеть, что из плеера вышли, НО старый код зависает из-за общего userConnections!
     * 5. При закрытии ВСЕХ потоков комната обязана:
     *    - Перевести онлайн в 0.
     *    - Уйти в LOBBY / PAUSED.
     *    - Сохранить ровно 150 секунд в БД.
     */
    @Test
    fun test_zombie_lobby_leak_and_clean_exit_auto_conserve() = runTest {
        val roomId = Uuid.random()
        val host = createAuthUser("HostUser")
        val guest = createAuthUser("GuestUser")

        val summary = WatchRoomSummaryDto(
            id = roomId,
            title = "Lioness Party",
            mediaId = "lioness_s1",
            mediaType = MediaType.TV,
            currentSeason = 1,
            currentEpisode = 1,
            lastPositionSeconds = 0L,
            participants = listOf(
                WatchRoomParticipantDto(userId = host.userId, username = host.username, role = WatchRoomParticipantRole.HOST, isOnline = false),
                WatchRoomParticipantDto(userId = guest.userId, username = guest.username, role = WatchRoomParticipantRole.MEMBER, isOnline = false)
            ),
            isHost = true,
            status = WatchRoomStatus.ACTIVE
        )

        val repo = BrutalFakeWatchRoomRepository(mutableListOf(summary))
        val manager = WatchRoomSessionManager(
            watchRoomRepository = repo,
            userMovieRepository = UserMovieRepository(),
            userEpisodeRepository = UserEpisodeRepository(),
            scope = backgroundScope
        )

        // Подписываемся на стрим комнат (как на экране карточек)
        val roomSummaries = mutableListOf<List<WatchRoomSummaryDto>>()
        val summaryJob = launch {
            manager.streamUserRooms(host.userId).collect {
                roomSummaries.add(it)
            }
        }
        runCurrent()
        assertEquals(0, roomSummaries.last().first().onlineParticipantsCount, "Изначально 0 онлайн")

        // 1. Клиенты открывают лобби
        val hostLobbyEvents = mutableListOf<LobbyEvent>()
        val hostLobbyJob = launch {
            manager.streamLobbyState(roomId, host)!!.collect { hostLobbyEvents.add(it) }
        }
        val guestLobbyEvents = mutableListOf<LobbyEvent>()
        val guestLobbyJob = launch {
            manager.streamLobbyState(roomId, guest)!!.collect { guestLobbyEvents.add(it) }
        }
        advanceTimeBy(150.milliseconds)
        runCurrent()

        val session = manager.getActiveSession(roomId)!!
        assertEquals(2, session.getOnlineCount(), "В лобби оба участника должны быть онлайн")

        // 2. Хост запускает воспроизведение
        manager.triggerStartPlayback(roomId, host.userId)
        runCurrent()
        assertEquals(RoomPhase.PREPARING, session.phase)

        // 3. Оба клиента открывают плеер (streamEvents)
        val hostPlayerEvents = mutableListOf<WatchRoomEvent>()
        val hostPlayerJob = launch {
            manager.streamEvents(roomId, host)!!.collect { hostPlayerEvents.add(it) }
        }
        val guestPlayerEvents = mutableListOf<WatchRoomEvent>()
        val guestPlayerJob = launch {
            manager.streamEvents(roomId, guest)!!.collect { guestPlayerEvents.add(it) }
        }
        runCurrent()

        // Рапортуем готовность медиа
        manager.sendPlaybackCommand(roomId, host.userId, RoomPlaybackCommand.ReportMediaReady(0L))
        manager.sendPlaybackCommand(roomId, guest.userId, RoomPlaybackCommand.ReportMediaReady(0L))
        runCurrent()

        // Ждем старта воспроизведения (1500мс lead time)
        advanceTimeBy(1600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase, "Комната должна быть в PLAYING_IN_SYNC")

        // 4. Перематываем на 150 секунд (2:30)
        manager.sendPlaybackCommand(roomId, host.userId, RoomPlaybackCommand.Seek(150_000L))
        advanceTimeBy(2100.milliseconds)
        runCurrent()
        assertEquals(150_000L, session.anchorPositionMs, "Позиция должна встать на 150 секунд")

        // 5. ДЕМОНСТРАЦИЯ БАГА / ПРОВЕРКА УТЕЧКИ:
        // Если закрыть ТОЛЬКО плеерные сокеты (hostPlayerJob и guestPlayerJob),
        // но оставить открытыми лобби-сокеты (hostLobbyJob и guestLobbyJob):
        hostPlayerJob.cancelAndJoin()
        guestPlayerJob.cancelAndJoin()
        advanceTimeBy(150.milliseconds)
        runCurrent()

        // В багнутой системе лобби-сокеты удерживали online = 2!
        println("Текущий онлайн при открытых лобби-сокетах: ${session.getOnlineCount()}")

        // Теперь закрываем лобби-сокеты (как должно быть при нормальном закрытии)
        hostLobbyJob.cancelAndJoin()
        guestLobbyJob.cancelAndJoin()
        advanceTimeBy(150.milliseconds)
        runCurrent()

        // ПРОВЕРЯЕМ СТРОГИЕ ИНВАРИАНТЫ:
        assertEquals(0, session.getOnlineCount(), "Все участники должны быть оффлайн!")
        assertEquals(RoomPhase.LOBBY, session.phase, "Комната обязана автоматически вернуться в LOBBY!")
        assertEquals(150_000L, session.anchorPositionMs, "Позиция НЕ должна сбрасываться в 0L!")
        assertEquals(150L, repo.lastSavedPositionSec, "Прогресс 150 сек должен быть сохранен в репозиторий!")

        val latestRoomSummary = roomSummaries.last().first()
        assertEquals(WatchRoomPhase.LOBBY, latestRoomSummary.phase)
        assertEquals(150L, latestRoomSummary.lastPositionSeconds)
        assertEquals(0, latestRoomSummary.onlineParticipantsCount)

        summaryJob.cancel()
    }

    /**
     * Сценарий 2: Повторный вход в сохраненную комнату.
     * Защита от сброса таймкода в 0:00 при подключении клиента с неинициализированным плеером.
     */
    @Test
    fun test_reopen_room_preserves_position_even_if_client_reports_zero() = runTest {
        val roomId = Uuid.random()
        val host = createAuthUser("HostUser")
        val guest = createAuthUser("GuestUser")

        // Изначально комната уже имеет сохраненный таймкод 300 секунд (5 минут)
        val initialPosition = 300L
        val summary = WatchRoomSummaryDto(
            id = roomId,
            title = "Lioness Party",
            mediaId = "lioness_s1",
            mediaType = MediaType.TV,
            currentSeason = 1,
            currentEpisode = 2,
            lastPositionSeconds = initialPosition,
            participants = listOf(
                WatchRoomParticipantDto(userId = host.userId, username = host.username, role = WatchRoomParticipantRole.HOST, isOnline = false),
                WatchRoomParticipantDto(userId = guest.userId, username = guest.username, role = WatchRoomParticipantRole.MEMBER, isOnline = false)
            ),
            isHost = true,
            status = WatchRoomStatus.PAUSED
        )

        val repo = BrutalFakeWatchRoomRepository(mutableListOf(summary))
        val manager = WatchRoomSessionManager(
            watchRoomRepository = repo,
            userMovieRepository = UserMovieRepository(),
            userEpisodeRepository = UserEpisodeRepository(),
            scope = backgroundScope
        )

        // 1. Клиенты подключаются к плееру сохраненной комнаты
        val session = manager.getOrCreateSession(roomId)!!
        assertEquals(initialPosition * 1000L, session.anchorPositionMs, "Сессия должна подняться с сохраненным временем 300с")

        val guestEvents = mutableListOf<WatchRoomEvent>()
        val guestJob = launch {
            manager.streamEvents(roomId, guest)!!.collect { guestEvents.add(it) }
        }
        val hostEvents = mutableListOf<WatchRoomEvent>()
        val hostJob = launch {
            manager.streamEvents(roomId, host)!!.collect { hostEvents.add(it) }
        }
        runCurrent()

        // Проверяем начальный снимок для подключающихся участников
        val initialSync = session.getCurrentSyncState()
        assertEquals(initialPosition * 1000L, initialSync.anchorPositionMs, "Начальный SyncState обязан содержать сохраненный таймкод 300с, а не 0с!")

        // 2. АТАКА: Клиент с багом (старый PlayerViewModel) присылает ReportMediaReady(0L)
        manager.sendPlaybackCommand(roomId, host.userId, RoomPlaybackCommand.ReportMediaReady(positionMs = 0L))
        runCurrent()

        // Проверяем: сервер НЕ должен был перезатереть anchorPositionMs нулем!
        assertEquals(initialPosition * 1000L, session.anchorPositionMs, "Сервер не должен сбрасывать сохраненную позицию в 0 при ReportMediaReady(0L)!")

        guestJob.cancelAndJoin()
        hostJob.cancelAndJoin()
        advanceTimeBy(150.milliseconds)
        runCurrent()

        assertEquals(0, session.getOnlineCount())
        assertEquals(RoomPhase.LOBBY, session.phase)
        assertEquals(initialPosition * 1000L, session.anchorPositionMs)
    }

    /**
     * Сценарий 3: БРУТАЛЬНЫЙ СТРЕСС-ТЕСТ (Stampede Chaos).
     * 15 клиентов, случайные дисконнекты, переключения серий, паузы, спам буферизацией.
     * Проверка на отсутствие дедлоков и корректный выход в LOBBY с сохраненным прогрессом.
     */
    @Test
    fun test_brutal_stampede_chaos_rapid_flapping() = runTest {
        val roomId = Uuid.random()
        val host = createAuthUser("HostUser")
        val guests = (1..14).map { createAuthUser("Guest_$it") }
        val allUsers = listOf(host) + guests

        val summary = WatchRoomSummaryDto(
            id = roomId,
            title = "Stampede Room",
            mediaId = "chaos_media",
            mediaType = MediaType.MOVIE,
            currentSeason = 1,
            currentEpisode = 1,
            lastPositionSeconds = 50L,
            participants = allUsers.map {
                WatchRoomParticipantDto(
                    userId = it.userId,
                    username = it.username,
                    role = if (it.userId == host.userId) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER,
                    isOnline = false
                )
            },
            isHost = true,
            status = WatchRoomStatus.ACTIVE
        )

        val repo = BrutalFakeWatchRoomRepository(mutableListOf(summary))
        val manager = WatchRoomSessionManager(
            watchRoomRepository = repo,
            userMovieRepository = UserMovieRepository(),
            userEpisodeRepository = UserEpisodeRepository(),
            scope = backgroundScope
        )

        val session = manager.getOrCreateSession(roomId)!!
        val activeJobs = mutableListOf<Job>()

        println("⚡ НАЧАЛО ШТОРМА ХАОСА: 15 клиентов подключаются к лобби и плееру...")

        // Этап 1: Все 15 подключаются к лобби
        allUsers.forEach { user ->
            val job = launch {
                manager.streamLobbyState(roomId, user)?.collect {}
            }
            activeJobs.add(job)
        }
        advanceTimeBy(150.milliseconds)
        runCurrent()
        assertEquals(15, session.getOnlineCount(), "Все 15 участников должны быть онлайн в лобби")

        // Этап 2: Хост запускает просмотр
        manager.triggerStartPlayback(roomId, host.userId)
        runCurrent()

        // Все 15 открывают плеер
        allUsers.forEach { user ->
            val job = launch {
                manager.streamEvents(roomId, user)?.collect {}
            }
            activeJobs.add(job)
        }
        runCurrent()

        // Рапортуют готовность
        allUsers.forEach { user ->
            manager.sendPlaybackCommand(roomId, user.userId, RoomPlaybackCommand.ReportMediaReady(50_000L))
        }
        advanceTimeBy(1600.milliseconds)
        runCurrent()
        assertEquals(RoomPhase.PLAYING_IN_SYNC, session.phase)

        // Этап 3: Хаотичный шторм (10 волн непредсказуемых событий)
        val random = Random(42)
        for (wave in 1..10) {
            // Случайный Seek от хоста
            val newSeekSec = random.nextLong(100L, 1000L)
            manager.sendPlaybackCommand(roomId, host.userId, RoomPlaybackCommand.Seek(newSeekSec * 1000L))

            // Случайные отчеты буферизации от половины участников
            guests.shuffled(random).take(5).forEach { g ->
                manager.sendPlaybackCommand(roomId, g.userId, RoomPlaybackCommand.ReportBuffer(isBuffering = true))
            }

            advanceTimeBy(200.milliseconds)
            runCurrent()

            // Резкий разрыв соединений у трети клиентов
            val toCancel = activeJobs.shuffled(random).take(random.nextInt(3, 8))
            toCancel.forEach {
                it.cancel()
                activeJobs.remove(it)
            }

            advanceTimeBy(300.milliseconds)
            runCurrent()
        }

        println("⚡ ШТОРМ ХАОСА ЗАВЕРШЕН. Принудительно отменяем оставшиеся ${activeJobs.size} корутин...")

        // Этап 4: Полное отключение всех участников
        activeJobs.forEach { it.cancelAndJoin() }
        activeJobs.clear()

        advanceTimeBy(500.milliseconds)
        runCurrent()

        // ПРОВЕРЯЕМ ФИНАЛЬНЫЕ ИНВАРИАНТЫ ЦЕЛОСТНОСТИ СИСТЕМЫ:
        println("Финальный статус сессии: phase=${session.phase}, onlineCount=${session.getOnlineCount()}, pos=${session.anchorPositionMs}ms")
        assertEquals(0, session.getOnlineCount(), "После закрытия всех сокетов онлайн должен быть строго 0!")
        assertEquals(RoomPhase.LOBBY, session.phase, "Комната обязана законсервироваться в фазе LOBBY!")
        assertTrue(session.anchorPositionMs > 0L, "Таймкод комнаты должен сохраниться положительным!")
        assertTrue(repo.lastSavedPositionSec > 0L, "Таймкод должен быть сброшен в БД при автоконсервации!")
    }

    /**
     * Сценарий 3: Реактивный стриминг комнат для тайтла (streamSavedRoomsForMedia).
     *
     * Проверяем, что:
     * 1. При подписке отдается начальный снимок.
     * 2. При создании новой комнаты для этого тайтла поток реактивно пушит свежий список.
     * 3. При подключении участника состав участников в списке комнат обновляется в реальном времени.
     * 4. При закрытии комнаты список обновляется реактивно.
     */
    @Test
    fun test_streamSavedRoomsForMedia_reactive_updates() = runTest {
        val repo = BrutalFakeWatchRoomRepository()
        val manager = WatchRoomSessionManager(
            watchRoomRepository = repo,
            userMovieRepository = UserMovieRepository(),
            userEpisodeRepository = UserEpisodeRepository(),
            scope = backgroundScope
        )

        val targetMediaId = "movie-matrix"
        val observerUser = createAuthUser("Observer")
        val hostUser = createAuthUser("Host")
        val guestUser = createAuthUser("Guest")

        val receivedLists = mutableListOf<List<WatchRoomSummaryDto>>()
        val collectJob = launch {
            manager.streamSavedRoomsForMedia(targetMediaId, observerUser.userId).collect {
                receivedLists.add(it)
            }
        }

        // 1. Начальный снимок — пусто
        runCurrent()
        assertEquals(1, receivedLists.size)
        assertTrue(receivedLists.last().isEmpty())

        // 2. Создаем комнату для целевого тайтла
        val roomId = Uuid.random()
        val roomDto = WatchRoomDto(
            id = roomId,
            title = "Матрица вместе",
            mediaId = targetMediaId,
            mediaType = MediaType.MOVIE,
            currentSeason = null,
            currentEpisode = null,
            lastPositionSeconds = 0L,
            hostUserId = hostUser.userId,
            controlMode = WatchRoomControlMode.HOST_ONLY,
            status = WatchRoomStatus.ACTIVE,
            joinPin = "112233",
            participants = listOf(
                WatchRoomParticipantDto(
                    userId = hostUser.userId,
                    username = hostUser.username,
                    role = WatchRoomParticipantRole.HOST,
                    isOnline = false
                )
            )
        )
        repo.rooms.add(
            WatchRoomSummaryDto(
                id = roomId,
                title = roomDto.title,
                mediaId = roomDto.mediaId,
                mediaType = roomDto.mediaType,
                currentSeason = null,
                currentEpisode = null,
                lastPositionSeconds = 0L,
                status = WatchRoomStatus.ACTIVE,
                phase = WatchRoomPhase.LOBBY,
                participants = roomDto.participants,
                isHost = false
            )
        )
        manager.registerNewRoom(roomDto, hostUser)

        advanceTimeBy(150.milliseconds)
        runCurrent()

        assertEquals(2, receivedLists.size)
        assertEquals(1, receivedLists.last().size)
        assertEquals("Матрица вместе", receivedLists.last().first().title)

        // 3. Гость входит в комнату
        manager.joinRoom(roomId, guestUser)

        advanceTimeBy(150.milliseconds)
        runCurrent()

        assertEquals(3, receivedLists.size)
        val updatedRoom = receivedLists.last().first()
        assertEquals(2, updatedRoom.participants.size)

        // 4. Хост закрывает комнату
        manager.closeRoom(roomId, hostUser.userId)

        advanceTimeBy(150.milliseconds)
        runCurrent()

        assertEquals(4, receivedLists.size)
        assertTrue(receivedLists.last().isEmpty(), "После закрытия комнаты список должен стать пустым!")

        collectJob.cancelAndJoin()
    }
}
