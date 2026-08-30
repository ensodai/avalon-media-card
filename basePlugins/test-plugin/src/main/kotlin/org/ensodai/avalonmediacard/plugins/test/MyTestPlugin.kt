package org.ensodai.avalonmediacard.plugins.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.*
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext

class MyTestPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.testplugin"
    override val name: String = "Тестовый Плагин"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    override fun onInitialize(context: PluginContext) {
        val logger = context.logger

        context.streams.onMedia { key, season, episode, userId ->
            flow {
                emit(
                    MediaStream(
                        title = "Поток 1: Зеркало CDN (Прямая ссылка)",
                        url = "https://example.com/video.mp4",
                        type = StreamType.DirectUrl,
                        quality = "1080p",
                        sourceName = "CDN Балансировщик"
                    )
                )

                delay(1000)

                emit(
                    MediaStream(
                        title = "Поток 2: Торрент-Magnet (Magnet Link)",
                        url = "magnet:?xt=urn:btih:d984f51f4adcb6b7b6b&dn=Movie",
                        type = StreamType.Magnet,
                        quality = "2160p (4K)",
                        sizeBytes = 15_000_000_000L,
                        sourceName = "RuTracker",
                        seeders = 145,
                        leechers = 12
                    )
                )

                delay(1500)

                emit(
                    MediaStream(
                        title = "Поток 3: HLS Stream (Адаптивный стриминг)",
                        url = "https://example.com/live/playlist.m3u8",
                        type = StreamType.Hls,
                        quality = "Auto",
                        sourceName = "VLC Streamer"
                    )
                )
            }
        }

        logger.info("Тестовый плагин инициализирован через Declarative Capabilities API!")
    }

    override fun onDestroy() {
        println("Тестовый плагин уничтожен!")
    }
}
