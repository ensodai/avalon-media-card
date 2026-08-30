package org.ensodai.avalonmediacard.domain.useCases.core

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.domain.repository.SduiRepository
import org.koin.core.annotation.Factory

@Factory
class StreamScreenSlotsUseCase(
    private val repository: SduiRepository
) {
    operator fun invoke(screen: Screen): Flow<org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent> {
        return repository.streamScreen(screen)
    }
}
