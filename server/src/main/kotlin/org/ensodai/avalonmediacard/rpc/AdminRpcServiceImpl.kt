package org.ensodai.avalonmediacard.rpc

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.ensodai.avalonmediacard.ServerRuntimeInfo
import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.CreateUserRequest
import org.ensodai.avalonmediacard.contract.admin.GlobalIntegrationSettingsDto
import org.ensodai.avalonmediacard.contract.admin.ServerSystemInfoDto
import org.ensodai.avalonmediacard.contract.admin.UpdateGlobalIntegrationSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UserDto
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.ensodai.avalonmediacard.contract.version.CoreVersion
import org.ensodai.avalonmediacard.database.DatabaseFactory
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.repository.MediaDiscoverCacheRepository
import org.ensodai.avalonmediacard.repository.MediaRepository
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserFeedCacheRepository
import org.ensodai.avalonmediacard.repository.UserRepository
import org.ensodai.avalonmediacard.security.PasswordHasher
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Base64
import kotlin.uuid.Uuid

@Factory
class AdminRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val pluginManager: PluginManager,
    private val userRepository: UserRepository,
    private val systemSettingsRepository: SystemSettingsRepository,
    private val tmdbApi: TmdbApi,
    private val httpClient: HttpClient,
    private val discoverCacheRepository: MediaDiscoverCacheRepository,
    private val userFeedCacheRepository: UserFeedCacheRepository,
    private val mediaRepository: MediaRepository
) : AdminRpcService {
    private val logger = LoggerFactory.getLogger(AdminRpcServiceImpl::class.java)

    private fun getCallerAdmin(): AuthState.Authorized {
        val authState = session.state.value
        if (authState !is AuthState.Authorized || authState.role != UserRole.ADMIN) {
            throw Exception("Forbidden: Administrator access required")
        }
        return authState
    }

    private fun requireAdmin() {
        getCallerAdmin()
    }

    override suspend fun uploadPlugin(fileName: String, fileContent: ByteArray): Boolean {
        requireAdmin()
        return try {
            val safeName = File(fileName).name
            if (!safeName.endsWith(".jar", ignoreCase = true)) {
                logger.warn("Rejected plugin upload with non-jar filename: {}", safeName)
                return false
            }
            val pluginsDir =
                if (File("server/plugins").exists()) File("server/plugins") else File("plugins").apply { mkdirs() }
            val file = File(pluginsDir, safeName).canonicalFile
            if (!file.parentFile.canonicalPath.equals(pluginsDir.canonicalPath)) {
                logger.warn("Path traversal attempt in plugin upload: {}", fileName)
                return false
            }
            file.writeBytes(fileContent)
            val loaded = pluginManager.loadPlugin(file)
            loaded.isNotEmpty()
        } catch (e: Exception) {
            logger.error("Failed to upload plugin: $fileName", e)
            false
        }
    }

    override suspend fun createUser(request: CreateUserRequest): AdminActionResponse {
        requireAdmin()
        
        val usernameTrimmed = request.username.trim()
        if (usernameTrimmed.length < 3) {
            return AdminActionResponse(success = false, error = "Имя пользователя должно быть не менее 3 символов")
        }
        if (request.passwordRaw.length < 4) {
            return AdminActionResponse(success = false, error = "Пароль должен быть не менее 4 символов")
        }

        val existingUser = userRepository.findByUsername(usernameTrimmed)
        if (existingUser != null) {
            return AdminActionResponse(success = false, error = "Имя пользователя уже занято")
        }

        val passwordHash = PasswordHasher.hash(request.passwordRaw)
        userRepository.createUser(usernameTrimmed, passwordHash, request.role)
        return AdminActionResponse(success = true)
    }

    override suspend fun getUsers(): List<UserDto> {
        requireAdmin()
        return userRepository.getAllUsers().map { 
            UserDto(
                id = it.id.toString(),
                username = it.username,
                role = it.role,
                status = it.status
            )
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): AdminActionResponse {
        val caller = getCallerAdmin()
        if (caller.userId.toString() == userId && status == UserStatus.FROZEN) {
            return AdminActionResponse(success = false, error = "Вы не можете заморозить собственный аккаунт")
        }
        return try {
            val id = Uuid.parse(userId)
            val success = userRepository.updateStatus(id, status)
            if (success) {
                AdminActionResponse(success = true)
            } else {
                AdminActionResponse(success = false, error = "Пользователь не найден")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = e.message)
        }
    }

    override suspend fun updateUserRole(userId: String, role: UserRole): AdminActionResponse {
        val caller = getCallerAdmin()
        if (caller.userId.toString() == userId && role != UserRole.ADMIN) {
            return AdminActionResponse(success = false, error = "Вы не можете отозвать права администратора у собственного аккаунта")
        }
        return try {
            val id = Uuid.parse(userId)
            val success = userRepository.updateRole(id, role)
            if (success) {
                AdminActionResponse(success = true)
            } else {
                AdminActionResponse(success = false, error = "Пользователь не найден")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = e.message)
        }
    }

    override suspend fun deleteUser(userId: String): AdminActionResponse {
        val caller = getCallerAdmin()
        if (caller.userId.toString() == userId) {
            return AdminActionResponse(success = false, error = "Вы не можете удалить собственный аккаунт")
        }
        return try {
            val id = Uuid.parse(userId)
            val success = userRepository.deleteUser(id)
            if (success) {
                AdminActionResponse(success = true)
            } else {
                AdminActionResponse(success = false, error = "Пользователь не найден")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = e.message)
        }
    }

    override suspend fun resetUserPassword(userId: String, newPasswordRaw: String): AdminActionResponse {
        requireAdmin()
        return try {
            val id = Uuid.parse(userId)
            val newHash = PasswordHasher.hash(newPasswordRaw)
            val success = userRepository.updatePasswordHashById(id, newHash)
            if (success) {
                AdminActionResponse(success = true)
            } else {
                AdminActionResponse(success = false, error = "Пользователь не найден")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = e.message)
        }
    }

    override suspend fun getGlobalIntegrationSettings(): GlobalIntegrationSettingsDto {
        requireAdmin()
        return GlobalIntegrationSettingsDto(
            tmdbReadToken = systemSettingsRepository.getSetting("tmdb_read_token"),
            tmdbShareWithUsers = systemSettingsRepository.getSetting("tmdb_share_with_users")?.toBooleanStrictOrNull() ?: true,
            torrServerHost = systemSettingsRepository.getSetting("torrserver_host"),
            torrServerLogin = systemSettingsRepository.getSetting("torrserver_login"),
            torrServerPassword = systemSettingsRepository.getSetting("torrserver_password"),
            torrServerShareWithUsers = systemSettingsRepository.getSetting("torrserver_share_with_users")?.toBooleanStrictOrNull() ?: false,
            torrServerUseGst = systemSettingsRepository.getSetting("torrserver_use_gst")?.toBooleanStrictOrNull() ?: false,
            prowlarrUrl = systemSettingsRepository.getSetting("prowlarr_url"),
            prowlarrApiKey = systemSettingsRepository.getSetting("prowlarr_api_key"),
            prowlarrShareWithUsers = systemSettingsRepository.getSetting("prowlarr_share_with_users")?.toBooleanStrictOrNull() ?: false,
            jackettUrl = systemSettingsRepository.getSetting("jackett_url"),
            jackettApiKey = systemSettingsRepository.getSetting("jackett_api_key"),
            jackettShareWithUsers = systemSettingsRepository.getSetting("jackett_share_with_users")?.toBooleanStrictOrNull() ?: false
        )
    }

    override suspend fun updateGlobalIntegrationSettings(request: UpdateGlobalIntegrationSettingsRequest): AdminActionResponse {
        requireAdmin()
        return try {
            request.tmdbReadToken?.let { 
                systemSettingsRepository.saveSetting("tmdb_read_token", it.trim()) 
            }
            request.tmdbShareWithUsers?.let {
                systemSettingsRepository.saveSetting("tmdb_share_with_users", it.toString())
            }
            request.torrServerHost?.let { 
                systemSettingsRepository.saveSetting("torrserver_host", it.trim()) 
            }
            request.torrServerLogin?.let { 
                systemSettingsRepository.saveSetting("torrserver_login", it.trim()) 
            }
            request.torrServerPassword?.let { 
                systemSettingsRepository.saveSetting("torrserver_password", it.trim()) 
            }
            request.torrServerShareWithUsers?.let {
                systemSettingsRepository.saveSetting("torrserver_share_with_users", it.toString())
            }
            request.torrServerUseGst?.let {
                systemSettingsRepository.saveSetting("torrserver_use_gst", it.toString())
            }
            request.prowlarrUrl?.let {
                systemSettingsRepository.saveSetting("prowlarr_url", it.trim())
            }
            request.prowlarrApiKey?.let {
                systemSettingsRepository.saveSetting("prowlarr_api_key", it.trim())
            }
            request.prowlarrShareWithUsers?.let {
                systemSettingsRepository.saveSetting("prowlarr_share_with_users", it.toString())
            }
            request.jackettUrl?.let {
                systemSettingsRepository.saveSetting("jackett_url", it.trim())
            }
            request.jackettApiKey?.let {
                systemSettingsRepository.saveSetting("jackett_api_key", it.trim())
            }
            request.jackettShareWithUsers?.let {
                systemSettingsRepository.saveSetting("jackett_share_with_users", it.toString())
            }
            AdminActionResponse(success = true)
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = e.message)
        }
    }

    override suspend fun testTmdbConnection(token: String): AdminActionResponse {
        requireAdmin()
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return AdminActionResponse(success = false, error = "Токен TMDB не указан")
        }
        return try {
            val isValid = tmdbApi.validateToken(cleanToken)
            if (isValid) {
                AdminActionResponse(success = true)
            } else {
                AdminActionResponse(success = false, error = "Не удалось авторизоваться в TMDB. Проверьте правильность токена.")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = "Ошибка при проверке TMDB: ${e.message}")
        }
    }

    override suspend fun testTorrServerConnection(host: String, login: String?, password: String?): AdminActionResponse {
        requireAdmin()
        val cleanHost = host.trim()
        if (cleanHost.isBlank()) {
            return AdminActionResponse(success = false, error = "URL хоста TorrServer не указан")
        }
        val url = if (cleanHost.startsWith("http://") || cleanHost.startsWith("https://")) cleanHost else "http://$cleanHost"
        val auth = if (!login.isNullOrBlank() && !password.isNullOrBlank()) {
            "Basic " + Base64.getEncoder().encodeToString("${login.trim()}:${password.trim()}".toByteArray())
        } else null
        return try {
            val response = httpClient.post("$url/settings") {
                contentType(ContentType.Application.Json)
                setBody("""{"action":"get"}""")
                if (auth != null) header(HttpHeaders.Authorization, auth)
                timeout { requestTimeoutMillis = 6000 }
            }
            if (response.status == HttpStatusCode.OK) {
                AdminActionResponse(success = true)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                AdminActionResponse(success = false, error = "Ошибка авторизации TorrServer: неверный логин или пароль")
            } else {
                AdminActionResponse(success = false, error = "TorrServer вернул код ошибки ${response.status.value}")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = "Не удалось подключиться к TorrServer: ${e.message ?: "Сервер недоступен"}")
        }
    }

    override suspend fun testProwlarrConnection(url: String, apiKey: String): AdminActionResponse {
        requireAdmin()
        val cleanUrl = url.trim().trimEnd('/')
        if (cleanUrl.isBlank()) {
            return AdminActionResponse(success = false, error = "URL Prowlarr не указан")
        }
        val fullUrl = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) cleanUrl else "http://$cleanUrl"
        return try {
            val response = httpClient.get("$fullUrl/api/v1/system/status") {
                header("X-Api-Key", apiKey.trim())
                timeout { requestTimeoutMillis = 6000 }
            }
            if (response.status == HttpStatusCode.OK) {
                AdminActionResponse(success = true)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                AdminActionResponse(success = false, error = "Ошибка авторизации Prowlarr: неверный API-ключ")
            } else {
                AdminActionResponse(success = false, error = "Prowlarr вернул код ошибки ${response.status.value}")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = "Не удалось подключиться к Prowlarr: ${e.message ?: "Сервер недоступен"}")
        }
    }

    override suspend fun testJackettConnection(url: String, apiKey: String): AdminActionResponse {
        requireAdmin()
        val cleanUrl = url.trim().trimEnd('/')
        if (cleanUrl.isBlank()) {
            return AdminActionResponse(success = false, error = "URL Jackett не указан")
        }
        val fullUrl = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) cleanUrl else "http://$cleanUrl"
        return try {
            val response = httpClient.get("$fullUrl/api/v2.0/indexers/all/results") {
                parameter("apikey", apiKey.trim())
                parameter("Query", "test_connection_dummy_123")
                timeout { requestTimeoutMillis = 6000 }
            }
            if (response.status == HttpStatusCode.OK) {
                AdminActionResponse(success = true)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                AdminActionResponse(success = false, error = "Ошибка авторизации Jackett: неверный API-ключ")
            } else {
                AdminActionResponse(success = false, error = "Jackett вернул код ошибки ${response.status.value}")
            }
        } catch (e: Exception) {
            AdminActionResponse(success = false, error = "Не удалось подключиться к Jackett: ${e.message ?: "Сервер недоступен"}")
        }
    }

    override suspend fun getSystemInfo(): ServerSystemInfoDto {
        requireAdmin()
        val isSqlite = DatabaseFactory.isSqlite
        val dbType = if (isSqlite) "SQLite (WAL mode)" else "PostgreSQL 17+"

        return ServerSystemInfoDto(
            coreVersion = CoreVersion.getDisplayVersion(),
            protocolVersion = CoreVersion.PROTOCOL_VERSION,
            buildDate = CoreVersion.BUILD_DATE,
            uptimeSeconds = ServerRuntimeInfo.getUptimeSeconds(),
            databaseType = dbType,
            activeUsersCount = userRepository.countActiveUsers(),
            totalUsersCount = userRepository.countTotalUsers(),
            cachedMediaCount = mediaRepository.count(),
            cachedDiscoverQueriesCount = discoverCacheRepository.count(),
            cachedUserFeedsCount = userFeedCacheRepository.count(),
            loadedPluginsCount = pluginManager.getLoadedPluginsCount(),
            javaVersion = "${System.getProperty("java.version")} (${System.getProperty("java.vendor") ?: "JVM"})",
            osName = "${System.getProperty("os.name")} ${System.getProperty("os.arch")}"
        )
    }

    override suspend fun clearDiscoverCache(): AdminActionResponse {
        requireAdmin()
        val count = discoverCacheRepository.clearAll()
        return AdminActionResponse(success = true, error = "Кэш выборок каталога очищен (удалено записей: $count)")
    }

    override suspend fun clearFeedCache(): AdminActionResponse {
        requireAdmin()
        userFeedCacheRepository.invalidateAll()
        return AdminActionResponse(success = true, error = "Кэш персональных лент пользователей успешно сброшен")
    }

    override suspend fun clearMediaCache(): AdminActionResponse {
        requireAdmin()
        val count = mediaRepository.clearAll()
        return AdminActionResponse(success = true, error = "Кэш метаданных медиа очищен (удалено записей: $count)")
    }
}
