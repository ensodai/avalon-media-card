package org.ensodai.avalonmediacard.core.player

object StreamUrlResolver {

    /**
     * Преобразует серверный WebSocket/RPC URL в базовый HTTP URL (без /api/rpc, /rpc, /api).
     */
    fun toBaseHttpUrl(serverUrl: String): String {
        return serverUrl
            .trim()
            .replace("ws://", "http://")
            .replace("wss://", "https://")
            .replace("://0.0.0.0", "://127.0.0.1")
            .substringBefore("/api")
            .substringBefore("/rpc")
            .trimEnd('/')
    }

    /**
     * Преобразует относительный путь (начинающийся с '/') в абсолютный HTTP URL.
     */
    fun resolveAbsoluteUrl(relativeOrAbsoluteUrl: String, serverUrl: String): String {
        if (!relativeOrAbsoluteUrl.startsWith("/")) return relativeOrAbsoluteUrl
        val base = toBaseHttpUrl(serverUrl)
        return "$base$relativeOrAbsoluteUrl"
    }

    fun resolve(
        rawUrl: String?,
        serverUrl: String?,
        fallbackServerUrl: String,
        audioTrackIndex: Int? = null
    ): String? {
        val url = rawUrl?.trim()
        if (url.isNullOrBlank()) return null

        val activeServerUrl = serverUrl?.takeIf { it.isNotBlank() } ?: fallbackServerUrl
        val baseResolved = resolveAbsoluteUrl(url, activeServerUrl)

        return if (isGstStream(baseResolved) && audioTrackIndex != null) {
            appendQueryParameter(baseResolved, "audio", audioTrackIndex.toString())
        } else {
            baseResolved
        }
    }

    fun isGstStream(url: String?): Boolean {
        return url != null && url.contains("/gst/")
    }

    private fun appendQueryParameter(url: String, key: String, value: String): String {
        return if (url.contains("?")) {
            if (url.contains("$key=")) {
                url.replace(Regex("$key=[^&]*"), "$key=$value")
            } else {
                "$url&$key=$value"
            }
        } else {
            "$url?$key=$value"
        }
    }
}
