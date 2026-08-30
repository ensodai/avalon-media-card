package org.ensodai.avalonmediacard.plugins.torrserver.domain.repository

import org.ensodai.avalonmediacard.contract.slot.ValidationStatus
import kotlin.uuid.Uuid

interface ValidationStateStore {
    fun setValidationStatus(userId: Uuid?, fieldKey: String, status: ValidationStatus, message: String?)
}
