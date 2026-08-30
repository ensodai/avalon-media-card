package org.ensodai.avalonmediacard.domain.useCases.core

import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.domain.repository.SduiRepository
import org.koin.core.annotation.Factory

@Factory
class ExecuteServerActionUseCase(
    private val repository: SduiRepository
) {
    suspend operator fun invoke(action: ServerAction): ActionResult {
        return repository.executeAction(action)
    }
}
