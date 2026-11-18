package org.ensodai.avalonmediacard.core

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@JsFun("() => document.visibilityState === 'visible'")
private external fun isDocumentVisibleJs(): Boolean

@JsFun("() => { if (document.fullscreenElement) return true; const v = document.querySelector('#video-underlay-container video') || document.querySelector('video'); return !!(v && !v.paused && !v.ended && v.readyState > 2); }")
private external fun isVideoPlayingOrFullscreenJs(): Boolean

@JsFun("() => { const m = document.querySelector(\"meta[name='avalon-build-hash']\"); return m ? (m.getAttribute('content') || '') : ''; }")
private external fun getLocalBuildHashJs(): String

@JsFun("() => { window.location.reload(); }")
private external fun reloadWindowJs()

@JsFun("() => String(Date.now())")
private external fun getNowTimestampJs(): String

class WasmAppUpdateManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : AppUpdateManager {

    private val _isUpdateAvailable = MutableStateFlow(false)
    override val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _newVersionName = MutableStateFlow<String?>(null)
    override val newVersionName: StateFlow<String?> = _newVersionName.asStateFlow()

    private val initialBuildHash: String = getLocalBuildHashJs().takeIf { it.isNotBlank() && it != "dev-local" } ?: ""

    private var dismissedHash: String? = null

    init {
        // 1. Слушаем visibilitychange: при возвращении пользователя во вкладку проверяем обновление
        document.addEventListener("visibilitychange", {
            if (isDocumentVisible()) {
                checkForUpdates()
            }
        })

        // 2. Фоновый опрос каждые 15-30 минут со случайным джиттером
        scope.launch {
            while (true) {
                val delayMinutes = Random.nextLong(15, 30)
                delay(delayMinutes.minutes)
                checkForUpdates()
            }
        }
    }

    override fun checkForUpdates() {
        if (initialBuildHash.isBlank()) return

        scope.launch {
            try {
                // Если активно воспроизводится видео или включен полноэкранный режим — не отвлекаем пользователя
                if (isVideoPlayingOrFullscreenJs()) {
                    return@launch
                }

                val timestamp = getNowTimestampJs()
                val response = window.fetch("/version.json?t=$timestamp").await()
                if (response.status.toInt() == 200) {
                    val text = response.text().await().toString()
                    val remoteHash = extractJsonField(text, "buildHash")
                    val remoteVersion = extractJsonField(text, "appVersion")

                    if (!remoteHash.isNullOrBlank() && remoteHash != initialBuildHash && remoteHash != dismissedHash) {
                        _newVersionName.value = remoteVersion
                        _isUpdateAvailable.value = true
                    }
                }
            } catch (_: Throwable) {
                // Ошибки сети игнорируются при тихой проверке
            }
        }
    }

    override fun applyUpdate() {
        reloadWindowJs()
    }

    override fun dismissUpdate() {
        _isUpdateAvailable.value = false
        dismissedHash = _newVersionName.value
    }

    private fun isDocumentVisible(): Boolean = isDocumentVisibleJs()

    private fun extractJsonField(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1)
    }
}

actual fun createPlatformUpdateManager(): AppUpdateManager = WasmAppUpdateManager()
