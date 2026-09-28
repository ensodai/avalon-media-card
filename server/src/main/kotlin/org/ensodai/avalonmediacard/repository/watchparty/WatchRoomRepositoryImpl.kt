package org.ensodai.avalonmediacard.repository.watchparty

import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.model.MediaImageType
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.database.MediaImageTable
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.MediaTranslationTable
import org.ensodai.avalonmediacard.database.UserTable
import org.ensodai.avalonmediacard.database.WatchRoomParticipantTable
import org.ensodai.avalonmediacard.database.WatchRoomTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

@Single
class WatchRoomRepositoryImpl : WatchRoomRepository {

    private val logger = LoggerFactory.getLogger(WatchRoomRepositoryImpl::class.java)

    override suspend fun createRoom(
        hostUserId: Uuid,
        request: CreateRoomRequest,
        pin: String
    ): WatchRoomDto = dbQuery {
        val newRoomId = Uuid.random()

        // 1. Убеждаемся, что тайтл присутствует в MediaTable для внешнего ключа
        val internalMediaId = MediaTable.selectAll()
            .where { MediaTable.externalId eq request.mediaId }
            .limit(1)
            .map { it[MediaTable.id].value }
            .singleOrNull() ?: run {
                val generatedId = Uuid.random()
                MediaTable.insertIgnore {
                    it[id] = generatedId
                    it[catalogId] = "tmdb"
                    it[externalId] = request.mediaId
                    it[mediaType] = if (request.mediaType == MediaType.MOVIE) "movie" else "tv"
                }
                MediaTable.selectAll()
                    .where { MediaTable.externalId eq request.mediaId }
                    .limit(1)
                    .map { it[MediaTable.id].value }
                    .single()
            }

        val roomTitle = request.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Комната просмотра"

        // 2. Создаем саму комнату
        WatchRoomTable.insert {
            it[id] = newRoomId
            it[title] = roomTitle
            it[mediaId] = internalMediaId
            it[mediaType] = request.mediaType
            it[currentSeason] = request.season
            it[currentEpisode] = request.episode
            it[lastPositionSeconds] = request.startPositionSeconds
            it[this.hostUserId] = hostUserId
            it[sourceType] = request.sourceType
            it[sourceId] = request.sourceId
            it[joinPin] = pin
            it[controlMode] = request.controlMode
            it[status] = WatchRoomStatus.ACTIVE
            it[isPrivate] = request.isPrivate
        }

        // 3. Добавляем хоста в постоянные участники
        WatchRoomParticipantTable.insertIgnore {
            it[id] = Uuid.random()
            it[roomId] = newRoomId
            it[userId] = hostUserId
            it[role] = WatchRoomParticipantRole.HOST
        }

        val hostUsername = UserTable.selectAll()
            .where { UserTable.id eq hostUserId }
            .map { it[UserTable.username] }
            .singleOrNull() ?: "Хост"

        val hostParticipant = WatchRoomParticipantDto(
            userId = hostUserId,
            username = hostUsername,
            role = WatchRoomParticipantRole.HOST,
            isOnline = true,
            playbackState = WatchRoomPlaybackState.READY
        )

        logger.info("Watch party room created: id={}, pin={}, mediaId={}", newRoomId, pin, request.mediaId)

        val (backdrops, titles) = loadMediaMeta(listOf(internalMediaId))

        WatchRoomDto(
            id = newRoomId,
            title = roomTitle,
            mediaId = request.mediaId,
            mediaType = request.mediaType,
            currentSeason = request.season,
            currentEpisode = request.episode,
            lastPositionSeconds = request.startPositionSeconds,
            hostUserId = hostUserId,
            sourceType = request.sourceType,
            sourceId = request.sourceId,
            joinPin = pin,
            controlMode = request.controlMode,
            status = WatchRoomStatus.ACTIVE,
            isPrivate = request.isPrivate,
            participants = listOf(hostParticipant),
            backdropUrl = backdrops[internalMediaId],
            mediaTitle = titles[internalMediaId]
        )
    }

    override suspend fun findRoomById(roomId: Uuid): WatchRoomDto? = dbQuery {
        val row = (WatchRoomTable innerJoin MediaTable)
            .selectAll()
            .where { WatchRoomTable.id eq roomId }
            .singleOrNull() ?: return@dbQuery null

        val participants = loadParticipantsInternal(roomId)
        val internalMediaId = row[WatchRoomTable.mediaId].value
        val (backdrops, titles) = loadMediaMeta(listOf(internalMediaId))
        mapToWatchRoomDto(
            row = row,
            participants = participants,
            backdropUrl = backdrops[internalMediaId],
            mediaTitle = titles[internalMediaId]
        )
    }

    override suspend fun findRoomByPin(pin: String): WatchRoomDto? = dbQuery {
        val row = (WatchRoomTable innerJoin MediaTable)
            .selectAll()
            .where {
                (WatchRoomTable.joinPin eq pin) and
                        (WatchRoomTable.status neq WatchRoomStatus.ARCHIVED)
            }
            .singleOrNull() ?: return@dbQuery null

        val roomId = row[WatchRoomTable.id].value
        val participants = loadParticipantsInternal(roomId)
        val internalMediaId = row[WatchRoomTable.mediaId].value
        val (backdrops, titles) = loadMediaMeta(listOf(internalMediaId))
        mapToWatchRoomDto(
            row = row,
            participants = participants,
            backdropUrl = backdrops[internalMediaId],
            mediaTitle = titles[internalMediaId]
        )
    }

    override suspend fun getRoomsForMedia(
        mediaId: String,
        currentUserId: Uuid,
        limit: Int,
        offset: Long
    ): List<WatchRoomSummaryDto> = dbQuery {
        val internalMediaId = MediaTable.selectAll()
            .where { MediaTable.externalId eq mediaId }
            .limit(1)
            .map { it[MediaTable.id].value }
            .singleOrNull() ?: return@dbQuery emptyList()

        val rooms = (WatchRoomTable innerJoin MediaTable)
            .selectAll()
            .where {
                (WatchRoomTable.mediaId eq internalMediaId) and
                        (WatchRoomTable.status neq WatchRoomStatus.ARCHIVED)
            }
            .orderBy(WatchRoomTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .toList()

        if (rooms.isEmpty()) return@dbQuery emptyList()

        val roomIds = rooms.map { it[WatchRoomTable.id].value }
        val mediaIds = rooms.map { it[WatchRoomTable.mediaId].value }.distinct()
        val (backdrops, titles) = loadMediaMeta(mediaIds)

        val participantsByRoom = (WatchRoomParticipantTable innerJoin UserTable).selectAll()
            .where { WatchRoomParticipantTable.roomId inList roomIds }
            .groupBy { it[WatchRoomParticipantTable.roomId].value }

        rooms.mapNotNull { row ->
            val roomId = row[WatchRoomTable.id].value
            val isHost = row[WatchRoomTable.hostUserId] == currentUserId
            val isPrivate = row[WatchRoomTable.isPrivate]

            val participantsInRoom = participantsByRoom[roomId].orEmpty().map { partRow ->
                WatchRoomParticipantDto(
                    userId = partRow[WatchRoomParticipantTable.userId],
                    username = partRow[UserTable.username],
                    role = partRow[WatchRoomParticipantTable.role],
                    isOnline = false,
                    playbackState = WatchRoomPlaybackState.READY
                )
            }
            val isParticipant = participantsInRoom.any { it.userId == currentUserId }

            // Если комната приватная, показываем её только участникам и хосту
            if (isPrivate && !isHost && !isParticipant) {
                return@mapNotNull null
            }

            val currentMediaId = row[WatchRoomTable.mediaId].value

            WatchRoomSummaryDto(
                id = roomId,
                title = row[WatchRoomTable.title],
                mediaId = row[MediaTable.externalId],
                mediaType = row[WatchRoomTable.mediaType],
                currentSeason = row[WatchRoomTable.currentSeason],
                currentEpisode = row[WatchRoomTable.currentEpisode],
                lastPositionSeconds = row[WatchRoomTable.lastPositionSeconds],
                joinPin = row[WatchRoomTable.joinPin],
                participants = participantsInRoom,
                isHost = isHost,
                status = row[WatchRoomTable.status],
                sourceType = row[WatchRoomTable.sourceType],
                sourceId = row[WatchRoomTable.sourceId],
                backdropUrl = backdrops[currentMediaId],
                mediaTitle = titles[currentMediaId]
            )
        }
    }

    override suspend fun getRoomsForUser(
        userId: Uuid,
        limit: Int,
        offset: Long
    ): List<WatchRoomSummaryDto> = dbQuery {
        val participantRoomIds = WatchRoomParticipantTable.selectAll()
            .where { WatchRoomParticipantTable.userId eq userId }
            .map { it[WatchRoomParticipantTable.roomId].value }
            .toSet()

        val whereOp = if (participantRoomIds.isNotEmpty()) {
            (WatchRoomTable.status neq WatchRoomStatus.ARCHIVED) and
                    ((WatchRoomTable.hostUserId eq userId) or (WatchRoomTable.id inList participantRoomIds))
        } else {
            (WatchRoomTable.status neq WatchRoomStatus.ARCHIVED) and
                    (WatchRoomTable.hostUserId eq userId)
        }

        val rooms = (WatchRoomTable innerJoin MediaTable)
            .selectAll()
            .where { whereOp }
            .orderBy(WatchRoomTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .toList()

        if (rooms.isEmpty()) return@dbQuery emptyList()

        val roomIds = rooms.map { it[WatchRoomTable.id].value }
        val mediaIds = rooms.map { it[WatchRoomTable.mediaId].value }.distinct()
        val (backdrops, titles) = loadMediaMeta(mediaIds)

        val participantsByRoom = (WatchRoomParticipantTable innerJoin UserTable).selectAll()
            .where { WatchRoomParticipantTable.roomId inList roomIds }
            .groupBy { it[WatchRoomParticipantTable.roomId].value }

        rooms.map { row ->
            val roomId = row[WatchRoomTable.id].value
            val isHost = row[WatchRoomTable.hostUserId] == userId
            val participantsInRoom = participantsByRoom[roomId].orEmpty().map { partRow ->
                WatchRoomParticipantDto(
                    userId = partRow[WatchRoomParticipantTable.userId],
                    username = partRow[UserTable.username],
                    role = partRow[WatchRoomParticipantTable.role],
                    isOnline = false,
                    playbackState = WatchRoomPlaybackState.READY
                )
            }

            val currentMediaId = row[WatchRoomTable.mediaId].value

            WatchRoomSummaryDto(
                id = roomId,
                title = row[WatchRoomTable.title],
                mediaId = row[MediaTable.externalId],
                mediaType = row[WatchRoomTable.mediaType],
                currentSeason = row[WatchRoomTable.currentSeason],
                currentEpisode = row[WatchRoomTable.currentEpisode],
                lastPositionSeconds = row[WatchRoomTable.lastPositionSeconds],
                joinPin = row[WatchRoomTable.joinPin],
                participants = participantsInRoom,
                isHost = isHost,
                status = row[WatchRoomTable.status],
                sourceType = row[WatchRoomTable.sourceType],
                sourceId = row[WatchRoomTable.sourceId],
                backdropUrl = backdrops[currentMediaId],
                mediaTitle = titles[currentMediaId]
            )
        }
    }

    override suspend fun updateRoomProgress(
        roomId: Uuid,
        season: Int?,
        episode: Int?,
        positionSeconds: Long
    ) = dbQuery {
        WatchRoomTable.update({ WatchRoomTable.id eq roomId }) {
            it[currentSeason] = season
            it[currentEpisode] = episode
            it[lastPositionSeconds] = positionSeconds
        }
        Unit
    }

    override suspend fun updateRoomStatus(
        roomId: Uuid,
        status: WatchRoomStatus
    ) = dbQuery {
        WatchRoomTable.update({ WatchRoomTable.id eq roomId }) {
            it[this.status] = status
            if (status == WatchRoomStatus.ARCHIVED) {
                it[joinPin] = null
            }
        }
        Unit
    }

    override suspend fun updateRoomControlMode(
        roomId: Uuid,
        controlMode: WatchRoomControlMode
    ) = dbQuery {
        WatchRoomTable.update({ WatchRoomTable.id eq roomId }) {
            it[this.controlMode] = controlMode
        }
        Unit
    }

    override suspend fun addParticipant(
        roomId: Uuid,
        userId: Uuid,
        role: WatchRoomParticipantRole
    ): Boolean = dbQuery {
        val exists = WatchRoomParticipantTable.selectAll()
            .where { (WatchRoomParticipantTable.roomId eq roomId) and (WatchRoomParticipantTable.userId eq userId) }
            .any()

        if (exists) {
            WatchRoomParticipantTable.update({
                (WatchRoomParticipantTable.roomId eq roomId) and (WatchRoomParticipantTable.userId eq userId)
            }) {
                it[this.role] = role
            }
        } else {
            WatchRoomParticipantTable.insertIgnore {
                it[id] = Uuid.random()
                it[this.roomId] = roomId
                it[this.userId] = userId
                it[this.role] = role
            }
        }
        true
    }

    override suspend fun removeParticipant(roomId: Uuid, userId: Uuid): Boolean = dbQuery {
        val deleted = WatchRoomParticipantTable.deleteWhere {
            (WatchRoomParticipantTable.roomId eq roomId) and (WatchRoomParticipantTable.userId eq userId)
        }
        deleted > 0
    }

    override suspend fun getParticipants(roomId: Uuid): List<WatchRoomParticipantDto> = dbQuery {
        loadParticipantsInternal(roomId)
    }

    override suspend fun transferHost(roomId: Uuid, newHostUserId: Uuid): Boolean = dbQuery {
        WatchRoomTable.update({ WatchRoomTable.id eq roomId }) {
            it[hostUserId] = newHostUserId
        }
        WatchRoomParticipantTable.update({
            (WatchRoomParticipantTable.roomId eq roomId) and (WatchRoomParticipantTable.role eq WatchRoomParticipantRole.HOST)
        }) {
            it[role] = WatchRoomParticipantRole.MEMBER
        }
        WatchRoomParticipantTable.update({
            (WatchRoomParticipantTable.roomId eq roomId) and (WatchRoomParticipantTable.userId eq newHostUserId)
        }) {
            it[role] = WatchRoomParticipantRole.HOST
        }
        true
    }

    override suspend fun deleteRoom(roomId: Uuid): Boolean = dbQuery {
        WatchRoomTable.update({ WatchRoomTable.id eq roomId }) {
            it[status] = WatchRoomStatus.ARCHIVED
            it[joinPin] = null
        } > 0
    }

    private fun loadParticipantsInternal(roomId: Uuid): List<WatchRoomParticipantDto> {
        return (WatchRoomParticipantTable innerJoin UserTable)
            .selectAll()
            .where { WatchRoomParticipantTable.roomId eq roomId }
            .map { row ->
                WatchRoomParticipantDto(
                    userId = row[WatchRoomParticipantTable.userId],
                    username = row[UserTable.username],
                    role = row[WatchRoomParticipantRoleCol],
                    isOnline = true,
                    playbackState = WatchRoomPlaybackState.READY
                )
            }
    }

    private val WatchRoomParticipantRoleCol get() = WatchRoomParticipantTable.role

    private fun loadMediaMeta(internalMediaIds: Collection<Uuid>): Pair<Map<Uuid, String>, Map<Uuid, String>> {
        if (internalMediaIds.isEmpty()) return emptyMap<Uuid, String>() to emptyMap<Uuid, String>()
        val backdrops = MediaImageTable.selectAll()
            .where { (MediaImageTable.mediaId inList internalMediaIds) and (MediaImageTable.imageType inList listOf(MediaImageType.BACKDROP, MediaImageType.POSTER)) }
            .groupBy { it[MediaImageTable.mediaId].value }
            .mapValues { (_, images) ->
                val backdrop = images.firstOrNull { it[MediaImageTable.imageType] == MediaImageType.BACKDROP }
                val poster = images.firstOrNull { it[MediaImageTable.imageType] == MediaImageType.POSTER }
                (backdrop ?: poster)?.get(MediaImageTable.url)?.toProxyImageUrl("w1280") ?: ""
            }.filterValues { it.isNotBlank() }

        val titles = MediaTranslationTable.selectAll()
            .where { MediaTranslationTable.mediaId inList internalMediaIds }
            .groupBy { it[MediaTranslationTable.mediaId].value }
            .mapValues { (_, trans) ->
                trans.firstOrNull { it[MediaTranslationTable.language] == "ru" }?.get(MediaTranslationTable.title)
                    ?: trans.firstOrNull()?.get(MediaTranslationTable.title) ?: ""
            }.filterValues { it.isNotBlank() }

        return backdrops to titles
    }

    private fun mapToWatchRoomDto(
        row: ResultRow,
        participants: List<WatchRoomParticipantDto>,
        backdropUrl: String? = null,
        mediaTitle: String? = null
    ): WatchRoomDto {
        return WatchRoomDto(
            id = row[WatchRoomTable.id].value,
            title = row[WatchRoomTable.title],
            mediaId = row[MediaTable.externalId],
            mediaType = row[WatchRoomTable.mediaType],
            currentSeason = row[WatchRoomTable.currentSeason],
            currentEpisode = row[WatchRoomTable.currentEpisode],
            lastPositionSeconds = row[WatchRoomTable.lastPositionSeconds],
            hostUserId = row[WatchRoomTable.hostUserId],
            sourceType = row[WatchRoomTable.sourceType],
            sourceId = row[WatchRoomTable.sourceId],
            joinPin = row[WatchRoomTable.joinPin],
            controlMode = row[WatchRoomTable.controlMode],
            status = row[WatchRoomTable.status],
            isPrivate = row[WatchRoomTable.isPrivate],
            participants = participants,
            backdropUrl = backdropUrl,
            mediaTitle = mediaTitle
        )
    }
}
