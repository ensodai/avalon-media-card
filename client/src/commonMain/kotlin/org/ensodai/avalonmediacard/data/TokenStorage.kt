package org.ensodai.avalonmediacard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.UserRole

class TokenStorage(private val dataStore: DataStore<Preferences>) {
    private val tokenKey = stringPreferencesKey("jwt_token")
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val userRoleKey = stringPreferencesKey("user_role")
    private val userIdKey = stringPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl = _serverUrl.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole = _userRole.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId = _userId.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    var cachedToken: String? = null
        private set

    var cachedServerUrl: String? = null
        private set

    var cachedUserRole: UserRole? = null
        private set

    var cachedUserId: String? = null
        private set

    var cachedUsername: String? = null
        private set

    init {
        CoroutineScope(Dispatchers.Main).launch {
            dataStore.data.collect { prefs ->
                val token = prefs[tokenKey]
                val url = prefs[serverUrlKey]
                val role = prefs[userRoleKey]?.let { roleName -> runCatching { UserRole.valueOf(roleName) }.getOrNull() }
                val uid = prefs[userIdKey]
                val uname = prefs[usernameKey]

                cachedToken = token
                _token.value = token
                cachedServerUrl = url
                if (!url.isNullOrBlank()) {
                    platformServerUrl = url
                }
                _serverUrl.value = url
                cachedUserRole = role
                _userRole.value = role
                cachedUserId = uid
                _userId.value = uid
                cachedUsername = uname
                _username.value = uname
                _isLoaded.value = true
            }
        }
    }

    suspend fun saveToken(token: String, role: UserRole? = null, userId: String? = null, username: String? = null) {
        cachedToken = token
        if (role != null) {
            cachedUserRole = role
            _userRole.value = role
        }
        if (userId != null) {
            cachedUserId = userId
            _userId.value = userId
        }
        if (username != null) {
            cachedUsername = username
            _username.value = username
        }
        dataStore.edit {
            it[tokenKey] = token
            if (role != null) {
                it[userRoleKey] = role.name
            }
            if (userId != null) {
                it[userIdKey] = userId
            }
            if (username != null) {
                it[usernameKey] = username
            }
        }
    }

    suspend fun saveRole(role: UserRole) {
        cachedUserRole = role
        _userRole.value = role
        dataStore.edit {
            it[userRoleKey] = role.name
        }
    }

    suspend fun saveServerUrl(url: String) {
        val normalizedUrl = normalizeServerUrl(url)
        cachedServerUrl = normalizedUrl
        if (normalizedUrl.isNotBlank()) {
            platformServerUrl = normalizedUrl
        }
        dataStore.edit {
            it[serverUrlKey] = normalizedUrl
        }
    }

    private fun normalizeServerUrl(input: String): String {
        var url = input.trim()
        if (url.isEmpty()) return url

        if (!url.startsWith("ws://") && !url.startsWith("wss://") && !url.startsWith("http://") && !url.startsWith("https://")) {
            val isLocal = url.startsWith("localhost") ||
                url.startsWith("127.") ||
                url.startsWith("10.") ||
                url.startsWith("192.168.") ||
                url.startsWith("172.") ||
                url.contains(".local") ||
                url.contains(":")
            url = if (isLocal) "ws://$url" else "wss://$url"
        }
        url = url.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")

        if (url.endsWith("/api")) {
            url += "/rpc"
        } else if (url.endsWith("/api/")) {
            url += "rpc"
        } else if (!url.endsWith("/rpc") && !url.contains("/api")) {
            url = url.trimEnd('/') + "/api/rpc"
        }

        return url
    }

    suspend fun clearToken() {
        cachedToken = null
        cachedUserRole = null
        cachedUserId = null
        cachedUsername = null
        _token.value = null
        _userRole.value = null
        _userId.value = null
        _username.value = null
        dataStore.edit {
            it.remove(tokenKey)
            it.remove(userRoleKey)
            it.remove(userIdKey)
            it.remove(usernameKey)
        }
    }
}
