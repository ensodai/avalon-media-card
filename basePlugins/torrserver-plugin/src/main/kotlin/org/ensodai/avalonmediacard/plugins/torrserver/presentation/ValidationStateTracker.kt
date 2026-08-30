package org.ensodai.avalonmediacard.plugins.torrserver.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import org.ensodai.avalonmediacard.contract.slot.ValidationStatus

object ValidationStateTracker {
    val validationStates = MutableStateFlow<Map<String, Map<String, Pair<ValidationStatus, String?>>>>(emptyMap())

    fun setValidationStatus(userId: String?, fieldKey: String, status: ValidationStatus, message: String?) {
        val uid = userId ?: "global"
        val current = validationStates.value
        val userMap = current[uid]?.toMutableMap() ?: mutableMapOf()
        userMap[fieldKey] = status to message
        validationStates.value = current + (uid to userMap)
    }
}
