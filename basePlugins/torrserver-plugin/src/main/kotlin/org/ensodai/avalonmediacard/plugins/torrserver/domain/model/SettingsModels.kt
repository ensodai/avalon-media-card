package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

data class TorrServerSettingsUpdate(
    val useTorrServer: String? = null,
    val host: String? = null,
    val login: String? = null,
    val password: String? = null,
    val useTorrServerGst: String? = null
)

data class ProwlarrSettingsUpdate(
    val useProwlarr: String? = null,
    val url: String? = null,
    val apiKey: String? = null
)

data class JackettSettingsUpdate(
    val useJackett: String? = null,
    val url: String? = null,
    val apiKey: String? = null
)
