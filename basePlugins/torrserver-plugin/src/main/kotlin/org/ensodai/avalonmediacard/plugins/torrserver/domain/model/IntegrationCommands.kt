package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.TemplateAction

@Serializable
data class TestProwlarrConnectionCommand(
    @SerialName("prowlarr_url") val url: String = "",
    @SerialName("prowlarr_api_key") val apiKey: String = ""
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "prowlarr_url" -> copy(url = value.toString())
            "prowlarr_api_key" -> copy(apiKey = value.toString())
            else -> this
        }
    }
}

@Serializable
data class TestJackettConnectionCommand(
    @SerialName("jackett_url") val url: String = "",
    @SerialName("jackett_api_key") val apiKey: String = ""
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "jackett_url" -> copy(url = value.toString())
            "jackett_api_key" -> copy(apiKey = value.toString())
            else -> this
        }
    }
}

@Serializable
data class TestTorrServerConnectionCommand(
    @SerialName("torrserver_host") val host: String = "",
    @SerialName("torrserver_login") val login: String? = null,
    @SerialName("torrserver_password") val password: String? = null,
    @SerialName("use_torrserver_gst") val useGst: String? = null
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "torrserver_host" -> copy(host = value.toString())
            "torrserver_login" -> copy(login = value.toString())
            "torrserver_password" -> copy(password = value.toString())
            "use_torrserver_gst" -> copy(useGst = value.toString())
            else -> this
        }
    }
}

@Serializable
data class SaveTorrServerSettingsCommand(
    @SerialName("use_torrserver") val useTorrServer: String? = null,
    @SerialName("torrserver_host") val torrserverHost: String? = null,
    @SerialName("torrserver_login") val torrserverLogin: String? = null,
    @SerialName("torrserver_password") val torrserverPassword: String? = null,
    @SerialName("use_torrserver_gst") val useTorrServerGst: String? = null
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "use_torrserver" -> copy(useTorrServer = value.toString())
            "torrserver_host" -> copy(torrserverHost = value.toString())
            "torrserver_login" -> copy(torrserverLogin = value.toString())
            "torrserver_password" -> copy(torrserverPassword = value.toString())
            "use_torrserver_gst" -> copy(useTorrServerGst = value.toString())
            else -> this
        }
    }
}

@Serializable
data class SaveProwlarrSettingsCommand(
    @SerialName("use_prowlarr") val useProwlarr: String? = null,
    @SerialName("prowlarr_url") val prowlarrUrl: String? = null,
    @SerialName("prowlarr_api_key") val prowlarrApiKey: String? = null
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "use_prowlarr" -> copy(useProwlarr = value.toString())
            "prowlarr_url" -> copy(prowlarrUrl = value.toString())
            "prowlarr_api_key" -> copy(prowlarrApiKey = value.toString())
            else -> this
        }
    }
}

@Serializable
data class SaveJackettSettingsCommand(
    @SerialName("use_jackett") val useJackett: String? = null,
    @SerialName("jackett_url") val jackettUrl: String? = null,
    @SerialName("jackett_api_key") val jackettApiKey: String? = null
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "use_jackett" -> copy(useJackett = value.toString())
            "jackett_url" -> copy(jackettUrl = value.toString())
            "jackett_api_key" -> copy(jackettApiKey = value.toString())
            else -> this
        }
    }
}
