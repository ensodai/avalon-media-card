package org.ensodai.avalonmediacard.presentation.screens.player.model

enum class PlayerMode {
    STANDARD,      // Обычный одиночный просмотр: полный функционал, сохранение истории, отметки просмотра и оценки
    TEST_PREVIEW,  // Тестовый режим / Предпросмотр источника: без синхронизации истории, без кнопок оценки/статуса
    WATCH_PARTY    // Режим совместного просмотра (TrueSync)
}
