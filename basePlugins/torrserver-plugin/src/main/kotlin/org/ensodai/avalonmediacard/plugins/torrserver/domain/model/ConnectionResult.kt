package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

sealed class ConnectionResult {
    object Success : ConnectionResult()
    data class AuthError(val message: String? = null) : ConnectionResult()
    data class NetworkError(val message: String? = null) : ConnectionResult()
    data class UnknownError(val message: String? = null) : ConnectionResult()
}
