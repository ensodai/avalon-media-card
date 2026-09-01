package org.ensodai.avalonmediacard.plugin

import org.ensodai.avalonmediacard.contract.plugins.IntegrationSettingSource
import org.ensodai.avalonmediacard.contract.plugins.IntegrationSettingsManager
import org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting
import org.ensodai.avalonmediacard.contract.plugins.ResolvedSearchEngineSetting
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserIntegrationSettingsRepository
import kotlin.uuid.Uuid

class IntegrationSettingsManagerImpl(
    private val pluginId: String,
    private val systemSettingsRepository: SystemSettingsRepository,
    private val userIntegrationSettingsRepository: UserIntegrationSettingsRepository,
    private val userExternalAuthRepository: org.ensodai.avalonmediacard.repository.UserExternalAuthRepository,
    private val userSettingsRepository: org.ensodai.avalonmediacard.repository.UserSettingsRepository
) : IntegrationSettingsManager {

    override suspend fun getTmdbToken(userId: Uuid?): ResolvedIntegrationSetting? {
        if (userId != null) {
            val userAppToken = userSettingsRepository.getUserSettings(userId).tmdbReadToken
            if (!userAppToken.isNullOrBlank()) {
                return ResolvedIntegrationSetting(userAppToken, IntegrationSettingSource.PERSONAL)
            }
            val userToken = userIntegrationSettingsRepository.getSetting(userId, pluginId, "tmdb_read_token")
                ?: userIntegrationSettingsRepository.getSetting(userId, "core", "tmdb_read_token")
            if (!userToken.isNullOrBlank()) {
                return ResolvedIntegrationSetting(userToken, IntegrationSettingSource.PERSONAL)
            }
        }
        
        val allowSharedTmdb = systemSettingsRepository.getSetting("tmdb_share_with_users")?.toBooleanStrictOrNull() ?: false
        if (allowSharedTmdb) {
            val globalToken = systemSettingsRepository.getSetting("tmdb_read_token")
            if (!globalToken.isNullOrBlank()) {
                return ResolvedIntegrationSetting(globalToken, IntegrationSettingSource.SHARED)
            }
        }

        return null
    }

    override suspend fun getTorrServerHost(userId: Uuid?): ResolvedIntegrationSetting? {
        if (userId != null) {
            val userHost = userIntegrationSettingsRepository.getSetting(userId, pluginId, "torrserver_host")
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_host")
            if (!userHost.isNullOrBlank()) {
                return ResolvedIntegrationSetting(userHost, IntegrationSettingSource.PERSONAL)
            }
        }

        val allowSharedTorr = systemSettingsRepository.getSetting("torrserver_share_with_users")?.toBooleanStrictOrNull() ?: false
        if (allowSharedTorr) {
            val globalHost = systemSettingsRepository.getSetting("torrserver_host")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_host")
            if (!globalHost.isNullOrBlank()) {
                return ResolvedIntegrationSetting(globalHost, IntegrationSettingSource.SHARED)
            }
        }

        return null
    }

    override suspend fun getTorrServerAuth(userId: Uuid?): String? {
        // Find out if we are using personal or shared host
        val resolvedHost = getTorrServerHost(userId) ?: return null
        
        val (login, pass) = if (resolvedHost.source == IntegrationSettingSource.PERSONAL) {
            val l = userIntegrationSettingsRepository.getSetting(userId!!, pluginId, "torrserver_login")
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_login")
            val p = userIntegrationSettingsRepository.getSetting(userId, pluginId, "torrserver_password")
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_password")
            l to p
        } else {
            val l = systemSettingsRepository.getSetting("torrserver_login")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_login")
            val p = systemSettingsRepository.getSetting("torrserver_password")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_password")
            l to p
        }

        if (!login.isNullOrBlank() && !pass.isNullOrBlank()) {
            return "Basic " + java.util.Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        }
        return null
    }

    override suspend fun getTorrServerUseGst(userId: Uuid?): Boolean {
        val resolvedHost = getTorrServerHost(userId) ?: return false
        return if (resolvedHost.source == IntegrationSettingSource.PERSONAL) {
            userIntegrationSettingsRepository.getSetting(userId!!, pluginId, "use_torrserver_gst")?.toBooleanStrictOrNull()
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "use_torrserver_gst")?.toBooleanStrictOrNull()
                ?: false
        } else {
            systemSettingsRepository.getSetting("torrserver_use_gst")?.toBooleanStrictOrNull()
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:use_torrserver_gst")?.toBooleanStrictOrNull()
                ?: false
        }
    }

    override suspend fun getProwlarrSettings(userId: Uuid?): ResolvedSearchEngineSetting? {
        if (userId != null) {
            val userUse = userIntegrationSettingsRepository.getSetting(userId, pluginId, "use_prowlarr")?.toBooleanStrictOrNull()
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "use_prowlarr")?.toBooleanStrictOrNull()
                ?: false
            if (userUse) {
                val userUrl = userIntegrationSettingsRepository.getSetting(userId, pluginId, "prowlarr_url")
                    ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "prowlarr_url")
                    ?: "http://localhost:9696"
                val userKey = userIntegrationSettingsRepository.getSetting(userId, pluginId, "prowlarr_api_key")
                    ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "prowlarr_api_key")
                    ?: ""
                return ResolvedSearchEngineSetting(userUrl, userKey, IntegrationSettingSource.PERSONAL)
            }
        }

        val allowShared = systemSettingsRepository.getSetting("prowlarr_share_with_users")?.toBooleanStrictOrNull() ?: false
        if (allowShared) {
            val globalUrl = systemSettingsRepository.getSetting("prowlarr_url")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:prowlarr_url")
            val globalKey = systemSettingsRepository.getSetting("prowlarr_api_key")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:prowlarr_api_key")
                ?: ""
            if (!globalUrl.isNullOrBlank()) {
                return ResolvedSearchEngineSetting(globalUrl, globalKey, IntegrationSettingSource.SHARED)
            }
        }

        return null
    }

    override suspend fun getJackettSettings(userId: Uuid?): ResolvedSearchEngineSetting? {
        if (userId != null) {
            val userUse = userIntegrationSettingsRepository.getSetting(userId, pluginId, "use_jackett")?.toBooleanStrictOrNull()
                ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "use_jackett")?.toBooleanStrictOrNull()
                ?: false
            if (userUse) {
                val userUrl = userIntegrationSettingsRepository.getSetting(userId, pluginId, "jackett_url")
                    ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "jackett_url")
                    ?: "http://localhost:9117"
                val userKey = userIntegrationSettingsRepository.getSetting(userId, pluginId, "jackett_api_key")
                    ?: userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "jackett_api_key")
                    ?: ""
                return ResolvedSearchEngineSetting(userUrl, userKey, IntegrationSettingSource.PERSONAL)
            }
        }

        val allowShared = systemSettingsRepository.getSetting("jackett_share_with_users")?.toBooleanStrictOrNull() ?: false
        if (allowShared) {
            val globalUrl = systemSettingsRepository.getSetting("jackett_url")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:jackett_url")
            val globalKey = systemSettingsRepository.getSetting("jackett_api_key")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:jackett_api_key")
                ?: ""
            if (!globalUrl.isNullOrBlank()) {
                return ResolvedSearchEngineSetting(globalUrl, globalKey, IntegrationSettingSource.SHARED)
            }
        }

        return null
    }

    override suspend fun hasTraktAuth(userId: Uuid?): Boolean {
        if (userId == null) return false
        return userExternalAuthRepository.getToken(userId, org.ensodai.avalonmediacard.contract.model.IntegrationService.TRAKT) != null
    }
}
