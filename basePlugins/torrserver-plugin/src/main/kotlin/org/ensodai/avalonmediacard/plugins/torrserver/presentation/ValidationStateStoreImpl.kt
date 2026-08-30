package org.ensodai.avalonmediacard.plugins.torrserver.presentation

import org.ensodai.avalonmediacard.contract.slot.ValidationStatus
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.ValidationStateStore
import kotlin.uuid.Uuid

class ValidationStateStoreImpl : ValidationStateStore {
    override fun setValidationStatus(userId: Uuid?, fieldKey: String, status: ValidationStatus, message: String?) {
        ValidationStateTracker.setValidationStatus(userId?.toString(), fieldKey, status, message)
    }
}
