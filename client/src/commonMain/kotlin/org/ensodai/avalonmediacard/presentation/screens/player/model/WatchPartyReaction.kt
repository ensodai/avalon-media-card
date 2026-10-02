package org.ensodai.avalonmediacard.presentation.screens.player.model

/**
 * Модель реакции эмодзи в плеере совместного просмотра.
 */
data class WatchPartyReaction(
    val id: Long,
    val emoji: String,
    val senderUsername: String,
    val isFromMe: Boolean
)

/**
 * Набор реакций по умолчанию для совместного просмотра.
 */
val WATCH_PARTY_EMOJIS: List<String> = listOf(
    "😂", // Смех / Комедия
    "😱", // Шок / Скример
    "😡", // Злость / Негодование
    "😭", // Слезы / Драма
    "🔥", // Огонь / Эпик
    "❤️", // Любовь / Милота
    "🍿", // Попкорн / Вкусный момент
    "🍅"  // Фирменный помидор / Закидать помидорами
)
