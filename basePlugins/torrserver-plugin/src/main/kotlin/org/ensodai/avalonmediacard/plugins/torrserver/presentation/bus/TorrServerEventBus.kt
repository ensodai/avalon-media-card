package org.ensodai.avalonmediacard.plugins.torrserver.presentation.bus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class TorrServerEventBus {
    private val userDynamicFlows = ConcurrentHashMap<Uuid, MutableSharedFlow<SlotUpdate>>()

    fun flowForUser(userId: Uuid): MutableSharedFlow<SlotUpdate> {
        return userDynamicFlows.getOrPut(userId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
    }

    suspend fun emitToUser(userId: Uuid, update: SlotUpdate) {
        flowForUser(userId).emit(update)
    }
}
