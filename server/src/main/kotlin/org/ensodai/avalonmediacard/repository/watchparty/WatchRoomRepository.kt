package org.ensodai.avalonmediacard.repository.watchparty

import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import kotlin.uuid.Uuid

/**
 * Репозиторий для управления комнатами совместного просмотра и участниками в БД.
 */
interface WatchRoomRepository {
    /**
     * Создает новую комнату и добавляет хоста как первого участника.
     */
    suspend fun createRoom(
        hostUserId: Uuid,
        request: CreateRoomRequest,
        pin: String
    ): WatchRoomDto

    /**
     * Поиск комнаты по ее постоянному UUID.
     */
    suspend fun findRoomById(roomId: Uuid): WatchRoomDto?

    /**
     * Поиск комнаты по временному PIN-коду для быстрого подключения с ТВ/консолей.
     */
    suspend fun findRoomByPin(pin: String): WatchRoomDto?

    /**
     * Возвращает список сохраненных активных комнат для тайтла.
     */
    suspend fun getRoomsForMedia(
        mediaId: String,
        currentUserId: Uuid,
        limit: Int = 50,
        offset: Long = 0
    ): List<WatchRoomSummaryDto>

    /**
     * Возвращает список всех комнат пользователя (где он хост или участник).
     */
    suspend fun getRoomsForUser(
        userId: Uuid,
        limit: Int = 50,
        offset: Long = 0
    ): List<WatchRoomSummaryDto>

    /**
     * Обновляет сохраненный прогресс воспроизведения (сезон, эпизод, секунды).
     */
    suspend fun updateRoomProgress(
        roomId: Uuid,
        season: Int?,
        episode: Int?,
        positionSeconds: Long
    )

    /**
     * Обновляет медиа-источник комнаты (провайдер, sourceId, сезон, эпизод).
     */
    suspend fun updateRoomSource(
        roomId: Uuid,
        sourceType: String,
        sourceId: String,
        season: Int?,
        episode: Int?
    ): Boolean

    /**
     * Обновляет статус комнаты (ACTIVE, PAUSED, ARCHIVED).
     */
    suspend fun updateRoomStatus(roomId: Uuid, status: WatchRoomStatus)

    /**
     * Обновляет режим управления комнатой (HOST_ONLY, DEMOCRATIC).
     */
    suspend fun updateRoomControlMode(roomId: Uuid, controlMode: WatchRoomControlMode)

    /**
     * Добавляет участника в постоянный состав комнаты.
     */
    suspend fun addParticipant(
        roomId: Uuid,
        userId: Uuid,
        role: WatchRoomParticipantRole = WatchRoomParticipantRole.MEMBER
    ): Boolean

    /**
     * Удаляет участника из комнаты.
     */
    suspend fun removeParticipant(roomId: Uuid, userId: Uuid): Boolean

    /**
     * Возвращает список участников комнаты с их именами.
     */
    suspend fun getParticipants(roomId: Uuid): List<WatchRoomParticipantDto>

    /**
     * Передает права хоста другому участнику в БД.
     */
    suspend fun transferHost(roomId: Uuid, newHostUserId: Uuid): Boolean

    /**
     * Удаляет комнату (или архивирует ее).
     */
    suspend fun deleteRoom(roomId: Uuid): Boolean
}
