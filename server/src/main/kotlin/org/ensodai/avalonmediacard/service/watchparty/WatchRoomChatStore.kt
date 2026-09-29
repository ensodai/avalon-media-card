package org.ensodai.avalonmediacard.service.watchparty

import java.util.concurrent.ConcurrentHashMap
import org.ensodai.avalonmediacard.contract.model.WatchRoomChatMessageDto
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Ключ группировки сообщений чата комнаты по эпизоду / серии.
 */
data class EpisodeChatKey(
    val season: Int?,
    val episode: Int?
)

/**
 * In-Memory хранилище сообщений чата комнаты совместного просмотра.
 * Хранит сообщения в памяти на время жизненного цикла сессии с ротацией кольцевого буфера
 * для предотвращения утечек памяти при длительных сессиях.
 */
class WatchRoomChatStore(
    private val maxMessagesPerEpisode: Int = 5000
) {
    private val chatHistoryByEpisode = ConcurrentHashMap<EpisodeChatKey, MutableList<WatchRoomChatMessageDto>>()

    /**
     * Добавление нового сообщения в историю текущей серии.
     */
    fun addMessage(
        roomId: Uuid,
        senderUserId: Uuid,
        senderUsername: String,
        senderAvatarUrl: String?,
        text: String,
        playbackPositionMs: Long,
        season: Int?,
        episode: Int?,
        createdAt: Instant = Clock.System.now()
    ): WatchRoomChatMessageDto {
        val key = EpisodeChatKey(season, episode)
        val list = chatHistoryByEpisode.computeIfAbsent(key) { ArrayList() }
        val message = WatchRoomChatMessageDto(
            id = Uuid.random(),
            roomId = roomId,
            senderUserId = senderUserId,
            senderUsername = senderUsername,
            senderAvatarUrl = senderAvatarUrl,
            text = text,
            playbackPositionMs = playbackPositionMs,
            season = season,
            episode = episode,
            createdAt = createdAt
        )
        synchronized(list) {
            if (list.size >= maxMessagesPerEpisode) {
                list.removeAt(0)
            }
            list.add(message)
        }
        return message
    }

    /**
     * Получение снимка истории сообщений для конкретной серии.
     */
    fun getHistory(season: Int?, episode: Int?): List<WatchRoomChatMessageDto> {
        val key = EpisodeChatKey(season, episode)
        val list = chatHistoryByEpisode[key] ?: return emptyList()
        return synchronized(list) {
            list.toList()
        }
    }

    /**
     * Полная очистка памяти при закрытии сессии.
     */
    fun clearAll() {
        chatHistoryByEpisode.clear()
    }
}
