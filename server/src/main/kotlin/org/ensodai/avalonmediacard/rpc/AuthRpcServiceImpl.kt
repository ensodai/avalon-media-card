package org.ensodai.avalonmediacard.rpc

import org.ensodai.avalonmediacard.auth.IntegrationProvider
import org.ensodai.avalonmediacard.auth.OAuthProvider
import org.ensodai.avalonmediacard.contract.auth.AuthResponse
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.auth.LoginRequest
import org.ensodai.avalonmediacard.contract.auth.RegisterRequest
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.model.IntegrationStatus
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserRepository
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.security.JwtProvider
import org.ensodai.avalonmediacard.security.PasswordHasher
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class AuthRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val userRepository: UserRepository,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val oauthProviders: List<OAuthProvider>,
    private val integrationProviders: List<IntegrationProvider>
) : AuthRpcService {

    override suspend fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            ?: return AuthResponse(success = false, error = "Пользователь не найден")

        val storedHash = userRepository.getPasswordHashByUsername(request.username)
            ?: return AuthResponse(success = false, error = "Ошибка проверки пароля")

        if (!PasswordHasher.verify(request.password, storedHash)) {
            return AuthResponse(success = false, error = "Неверный пароль")
        }

        val token = JwtProvider.generateToken(user.id, user.username, user.role)
        session.updateState(
            AuthState.Authorized(
                userId = user.id,
                username = user.username,
                role = user.role
            )
        )
        return AuthResponse(
            success = true,
            token = token,
            role = user.role,
            userId = user.id.toString(),
            username = user.username
        )
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        val usernameTrimmed = request.username.trim()
        if (usernameTrimmed.length < 3) {
            return AuthResponse(success = false, error = "Имя пользователя должно быть не менее 3 символов")
        }
        if (request.password.length < 4) {
            return AuthResponse(success = false, error = "Пароль должен быть не менее 4 символов")
        }

        val existingUser = userRepository.findByUsername(usernameTrimmed)
        if (existingUser != null) {
            return AuthResponse(success = false, error = "Имя пользователя уже занято")
        }

        val passwordHash = PasswordHasher.hash(request.password)
        val userId = userRepository.createUser(usernameTrimmed, passwordHash, UserRole.USER)

        val token = JwtProvider.generateToken(userId, usernameTrimmed, UserRole.USER)
        session.updateState(
            AuthState.Authorized(
                userId = userId,
                username = usernameTrimmed,
                role = UserRole.USER
            )
        )
        return AuthResponse(
            success = true,
            token = token,
            role = UserRole.USER,
            userId = userId.toString(),
            username = usernameTrimmed
        )
    }

    override suspend fun authenticate(token: String): AuthResponse? {
        val payload = JwtProvider.verifyToken(token) ?: return null

        // Проверяем, существует ли пользователь в БД
        val user = userRepository.findById(payload.userId) ?: return null

        session.updateState(
            AuthState.Authorized(
                userId = user.id,
                username = user.username,
                role = user.role
            )
        )
        return AuthResponse(
            success = true,
            token = token,
            role = user.role,
            userId = user.id.toString(),
            username = user.username
        )
    }

    override suspend fun getOAuthUrl(service: String): String {
        val authState = session.state.value
        if (authState !is AuthState.Authorized) {
            throw Exception("Пользователь не авторизован")
        }
        val provider = oauthProviders.find { it.serviceName.equals(service, ignoreCase = true) }
            ?: throw IllegalArgumentException("Неподдерживаемый сервис: $service")
        return provider.getAuthUrl(state = service.lowercase())
    }

    override suspend fun exchangeOAuthCode(service: String, code: String): Boolean {
        val authState = session.state.value
        if (authState !is AuthState.Authorized) {
            throw Exception("Пользователь не авторизован")
        }
        val userId = authState.userId

        val provider = oauthProviders.find { it.serviceName.equals(service, ignoreCase = true) }
            ?: throw IllegalArgumentException("Неподдерживаемый сервис: $service")

        val tokenResponse = provider.exchangeCode(code)
        return userExternalAuthRepository.saveToken(
            userId = userId,
            service = IntegrationService.fromId(provider.serviceName) ?: IntegrationService.TRAKT,
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn
        )
    }

    override suspend fun getIntegrationsStatus(): List<IntegrationStatus> {
        val authState = session.state.value
        if (authState !is AuthState.Authorized) {
            return emptyList()
        }
        val userId = authState.userId

        return oauthProviders.map { provider ->
            val serviceEnum = IntegrationService.fromId(provider.serviceName) ?: IntegrationService.TRAKT
            val token = userExternalAuthRepository.getToken(userId, serviceEnum)
            if (token != null) {
                val username = provider.getUserProfile(token.accessToken)
                IntegrationStatus(
                    service = provider.serviceName,
                    connected = true,
                    username = username ?: "Аккаунт ${provider.displayName}",
                    displayName = provider.displayName,
                    description = provider.description,
                    comingSoon = provider.comingSoon
                )
            } else {
                IntegrationStatus(
                    service = provider.serviceName,
                    connected = false,
                    displayName = provider.displayName,
                    description = provider.description,
                    comingSoon = provider.comingSoon
                )
            }
        }
    }

    override suspend fun updateLocale(locale: String): Boolean {
        val state = session.state.value
        val userId = (state as? AuthState.Authorized)?.userId ?: return false
        userSettingsRepository.saveUserLocale(userId, locale)
        return true
    }
}
