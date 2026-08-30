package org.ensodai.avalonmediacard.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ensodai.avalonmediacard.contract.auth.AuthState
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

class RpcSessionContext {
    private val _state = MutableStateFlow<AuthState>(AuthState.Guest)
    val state = _state.asStateFlow()

    fun updateState(newState: AuthState) {
        _state.value = newState
    }

    suspend fun awaitUserId(): Uuid {
        return state.filterIsInstance<AuthState.Authorized>().first().userId
    }
}
