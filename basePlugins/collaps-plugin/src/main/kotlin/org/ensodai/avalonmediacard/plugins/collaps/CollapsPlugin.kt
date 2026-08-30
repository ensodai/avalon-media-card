package org.ensodai.avalonmediacard.plugins.collaps

import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.collaps.data.network.CollapsApiClient
import org.ensodai.avalonmediacard.plugins.collaps.data.repository.CollapsRepositoryImpl
import org.ensodai.avalonmediacard.plugins.collaps.domain.usecase.GetCollapsPlaylistUseCase
import org.ensodai.avalonmediacard.plugins.collaps.domain.usecase.SearchCollapsStreamsUseCase

/**
 * **Collaps CDN Online Plugin (Experimental / Proof of Concept)**
 *
 * ⚠️ **ВНИМАНИЕ / DISCLAIMER: НЕСТАБИЛЬНЫЙ ПРОВАЙДЕР**
 * Данный плагин работает с неофициальным пиратским видео-балансером (Collaps CDN / Ortified).
 * Подобные сервисы не предоставляют стабильного публичного API, регулярно меняют домены,
 * обновляют алгоритмы обфускации веб-плеера, внедряют антибот-защиту и ограничивают частые запросы (HTTP 422).
 * В связи с этим постоянная бесперебойная работа данного источника не гарантируется.
 *
 * 💡 **ЭТАЛОННАЯ АРХИТЕКТУРНАЯ РЕАЛИЗАЦИЯ (REFERENCE IMPLEMENTATION):**
 * Плагин служит полноценным примером того, как в экосистеме Avalon Media Card можно реализовать:
 * - Парсинг и извлечение прямых потоков (Direct MP4 / HLS) из веб-страниц;
 * - Маппинг нескольких аудиодорожек (дубляж, закадровые студии) и внешних WebVTT субтитров;
 * - Разбор мастер-манифестов HLS с выбором качества (1080p, 720p, 540p и т.д.);
 * - Динамическое разрешение DNAME-записей CDN и сквозное проксирование потокового видео
 *   через Ktor Server (Range Requests / HTTP 206 Partial Content);
 * - Поиск по каталогу с приоритетом точного сопоставления по IMDb ID.
 *
 * Разработчики и сообщество могут использовать этот плагин как шаблон для создания
 * и поддержки собственных парсеров онлайн-кинотеатров и видео-балансеров.
 */
class CollapsPlugin : AvalonPlugin {

    override val id: String = "collaps-plugin"
    override val name: String = "Collaps CDN"
    override val version: String = "1.0.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    override fun onInitialize(context: PluginContext) {
        logger = context.logger
        logger.info("Initializing Collaps CDN Plugin [v$version]")

        // 1. Data Layer: Network Client & Repository
        val apiClient = CollapsApiClient(context.httpClient, logger)
        val repository = CollapsRepositoryImpl(apiClient)

        // 2. Domain Layer: Use Cases
        val searchUseCase = SearchCollapsStreamsUseCase(context, repository)
        val playlistUseCase = GetCollapsPlaylistUseCase(context, repository)

        // 3. Streams Registration
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }

        context.streams.onPrepare { stream, _ ->
            stream
        }

        context.streams.onPlaylist { key, sourceId, userId ->
            playlistUseCase.execute(key, sourceId, userId)
        }

        logger.info("Collaps CDN Plugin successfully registered and ready!")
    }
}
