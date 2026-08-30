package org.ensodai.avalonmediacard.core.player

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
import java.io.File

interface MpvNative : Library {

    companion object {
        const val MPV_FORMAT_NONE = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_OSD_STRING = 2
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
        const val MPV_FORMAT_DOUBLE = 5

        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_PLAYBACK_RESTART = 21
        const val MPV_EVENT_PROPERTY_CHANGE = 22

        const val MPV_RENDER_PARAM_INVALID = 0
        const val MPV_RENDER_PARAM_API_TYPE = 1
        const val MPV_RENDER_PARAM_SW_SIZE = 17
        const val MPV_RENDER_PARAM_SW_FORMAT = 18
        const val MPV_RENDER_PARAM_SW_STRIDE = 19
        const val MPV_RENDER_PARAM_SW_POINTER = 20

        const val MPV_RENDER_API_TYPE_SW = "sw"

        interface LibC : Library {
            companion object {
                val INSTANCE: LibC? by lazy {
                    try {
                        if (Platform.isWindows()) {
                            Native.load("msvcrt", LibC::class.java)
                        } else {
                            Native.load("c", LibC::class.java)
                        }
                    } catch (e: Throwable) {
                        println("[LibC] Failed to load libc: ${e.message}")
                        null
                    }
                }

                fun initLocale() {
                    try {
                        val lcNumeric = if (Platform.isWindows()) 4 else 1
                        INSTANCE?.setlocale(lcNumeric, "C")
                        println("[MPV] Initialized LC_NUMERIC locale to C")
                    } catch (e: Throwable) {
                        println("[LibC] Failed to setlocale: ${e.message}")
                    }
                }
            }

            fun setlocale(category: Int, locale: String): String?
        }

        /**
         * Native libmpv loader instance.
         *
         * Windows:
         * Uses bundled `mpv-2.dll` from resources (`win32-x86-64/mpv-2.dll`).
         * Upstream repository for Windows prebuilt binaries:
         * - https://github.com/zhongfly/mpv-winbuild/releases (or https://github.com/shinchiro/mpv-winbuild-cmake/releases)
         * - Download: `mpv-dev-x86_64-*.7z` -> extract `libmpv-2.dll` -> rename to `mpv-2.dll` and put in `resources/win32-x86-64/`.
         *
         * Linux:
         * Uses local binary in `./mpv-libs/linux-x86-64/` or resources `linux-x86-64/`, falling back to system `libmpv.so.2` (requires `sudo apt install libmpv2`).
         */
        val INSTANCE: MpvNative by lazy {
            var loaded: MpvNative? = null
            var lastError: Throwable? = null

            val isWindows = Platform.isWindows()
            val isLinux = Platform.isLinux()

            // 1. Проверяем путь из переменной окружения MPV_LIB_PATH или JVM свойства mpv.lib.path
            val customEnvPath = System.getenv("MPV_LIB_PATH") ?: System.getProperty("mpv.lib.path")
            if (!customEnvPath.isNullOrBlank()) {
                val customFile = File(customEnvPath)
                if (customFile.exists()) {
                    try {
                        loaded = Native.load(customFile.absolutePath, MpvNative::class.java)
                        println("[MPV] Successfully loaded from MPV_LIB_PATH: ${customFile.absolutePath}")
                        return@lazy loaded
                    } catch (e: Throwable) {
                        println("[MPV] Failed to load from MPV_LIB_PATH ($customEnvPath): ${e.message}")
                        lastError = e
                    }
                }
            }

            // 2. Проверяем локальные папки mpv-libs рядом с рабочей директорией / проектом
            val localCandidates = when {
                isWindows -> listOf(
                    File("mpv-libs/win32-x86-64/mpv-2.dll"),
                    File("mpv-libs/win32-x86-64/libmpv-2.dll"),
                    File("mpv-libs/mpv-2.dll"),
                    File("mpv-2.dll")
                )
                isLinux -> listOf(
                    File("mpv-libs/linux-x86-64/libmpv.so.2"),
                    File("mpv-libs/linux-x86-64/libmpv.so"),
                    File("mpv-libs/libmpv.so.2"),
                    File("mpv-libs/libmpv.so"),
                    File("libmpv.so.2"),
                    File("libmpv.so")
                )
                else -> emptyList()
            }

            for (file in localCandidates) {
                if (file.exists()) {
                    try {
                        loaded = Native.load(file.absolutePath, MpvNative::class.java)
                        println("[MPV] Successfully loaded from local directory: ${file.absolutePath}")
                        return@lazy loaded
                    } catch (e: Throwable) {
                        println("[MPV] Failed to load from local file ${file.path}: ${e.message}")
                        lastError = e
                    }
                }
            }

            // 3. Проверяем встроенные ресурсы из classpath / JAR
            val resourcePaths = when {
                isWindows -> listOf("win32-x86-64/mpv-2.dll", "win32-x86-64/libmpv-2.dll")
                isLinux -> listOf("linux-x86-64/libmpv.so.2", "linux-x86-64/libmpv.so")
                else -> emptyList()
            }

            for (resPath in resourcePaths) {
                try {
                    val extractedFile = Native.extractFromResourcePath(resPath, MpvNative::class.java.classLoader)
                    if (extractedFile != null && extractedFile.exists()) {
                        loaded = Native.load(extractedFile.absolutePath, MpvNative::class.java)
                        println("[MPV] Successfully loaded bundled library from resources: $resPath -> ${extractedFile.absolutePath}")
                        return@lazy loaded
                    }
                } catch (e: Throwable) {
                    lastError = e
                }
            }

            // 4. Проверяем системные библиотеки ОС (стандартный динамический линковщик)
            val libNames = listOf("mpv", "mpv-2", "mpv-1", "libmpv", "libmpv.so.2", "libmpv.so.1", "libmpv.so")
            for (name in libNames) {
                try {
                    loaded = Native.load(name, MpvNative::class.java)
                    println("[MPV] Successfully loaded system native library: $name")
                    return@lazy loaded
                } catch (e: Throwable) {
                    lastError = e
                }
            }

            // 5. Если ничего не найдено, формируем подробное сообщение с инструкциями
            val errorMessage = buildString {
                appendLine("================================================================================")
                appendLine("[MPV Error] Failed to load native libmpv library.")
                if (isLinux) {
                    appendLine("🐧 How to fix on Linux:")
                    appendLine("1. Install via package manager:")
                    appendLine("   • Ubuntu / Debian / Mint : sudo apt install libmpv2 libmpv-dev")
                    appendLine("   • Arch / Manjaro         : sudo pacman -S mpv")
                    appendLine("   • Fedora / RHEL          : sudo dnf install mpv-libs")
                    appendLine("   • openSUSE               : sudo zypper install libmpv2")
                    appendLine("2. OR put custom libmpv.so.2 in './mpv-libs/linux-x86-64/' or 'resources/linux-x86-64/'")
                    appendLine("3. OR set environment variable: export MPV_LIB_PATH=/path/to/libmpv.so.2")
                } else if (isWindows) {
                    appendLine("🪟 How to fix on Windows:")
                    appendLine("1. Ensure mpv-2.dll is present in 'resources/win32-x86-64/' or './mpv-libs/win32-x86-64/'")
                    appendLine("2. OR set environment variable: set MPV_LIB_PATH=C:\\path\\to\\mpv-2.dll")
                }
                appendLine("================================================================================")
            }

            throw IllegalStateException(errorMessage, lastError)
        }
    }

    @Structure.FieldOrder("type", "data")
    open class MpvRenderParam : Structure {
        constructor() : super()
        constructor(p: Pointer) : super(p) { read() }

        @JvmField var type: Int = 0
        @JvmField var data: Pointer? = null
    }


    @Structure.FieldOrder("name", "format", "data")
    open class MpvEventProperty : Structure {
        constructor() : super()
        constructor(p: Pointer) : super(p) { read() }

        @JvmField var name: String? = null
        @JvmField var format: Int = 0
        @JvmField var data: Pointer? = null
    }

    @Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
    open class MpvEvent : Structure {
        constructor() : super()
        constructor(p: Pointer) : super(p) { read() }

        class ByReference : MpvEvent, Structure.ByReference {
            constructor() : super()
            constructor(p: Pointer) : super(p)
        }

        @JvmField var event_id: Int = 0
        @JvmField var error: Int = 0
        @JvmField var reply_userdata: Long = 0L
        @JvmField var data: Pointer? = null
    }

    @Structure.FieldOrder("prefix", "level", "text", "log_level")
    open class MpvEventLogMessage : Structure {
        constructor() : super()
        constructor(p: Pointer) : super(p) { read() }

        @JvmField var prefix: String? = null
        @JvmField var level: String? = null
        @JvmField var text: String? = null
        @JvmField var log_level: Int = 0
    }

    interface MpvRenderUpdateCallback : Callback {
        fun invoke(cb_ctx: Pointer?)
    }

    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_destroy(ctx: Pointer)
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_error_string(error: Int): String?
    fun mpv_request_log_messages(ctx: Pointer, min_level: String): Int
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_command_string(ctx: Pointer, args: String): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(data: Pointer)
    fun mpv_set_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int
    fun mpv_get_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int
    fun mpv_observe_property(ctx: Pointer, reply_userdata: Long, name: String, format: Int): Int
    fun mpv_unobserve_property(ctx: Pointer, registered_reply_userdata: Long): Int
    fun mpv_wait_event(ctx: Pointer, timeout: Double): MpvEvent.ByReference?

    fun mpv_render_context_create(res: PointerByReference, mpv: Pointer, params: Pointer?): Int
    fun mpv_render_context_set_update_callback(ctx: Pointer, callback: MpvRenderUpdateCallback?, cb_ctx: Pointer?)
    fun mpv_render_context_update(ctx: Pointer): Long
    fun mpv_render_context_render(ctx: Pointer, params: Pointer): Int
    fun mpv_render_context_report_swap(ctx: Pointer)
    fun mpv_render_context_free(ctx: Pointer)
}
