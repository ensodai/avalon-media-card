package org.ensodai.avalonmediacard.presentation.core

import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.slot.SlotState
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate

/**
 * Универсальный контейнер-слот для SDUI.
 * Содержит в себе идентификатор ноды (для списков Compose) и типизированный стейт.
 * 
 * В этот слот мапятся сырые обновления [SlotUpdate], приходящие с сервера,
 * чтобы Умные Виджеты (например, `MovieCarousel`) могли их отрисовать.
 */
data class SduiSlot<T>(
    val nodeId: String,
    val state: SlotUiState<T>
)

/**
 * Конвертирует сырой [SlotUpdate] с сервера в типизированный [SduiSlot].
 */
inline fun <reified T : SlotData> SlotUpdate.toSduiSlot(oldData: T? = null): SduiSlot<T> {
    val uiState = when (val state = this.state) {
        is SlotState.Loading -> SlotUiState(isLoading = true, data = oldData)
        is SlotState.Content -> {
            val typedData = state.data as? T
            if (typedData != null) SlotUiState(data = typedData) else SlotUiState(data = oldData)
        }

        is SlotState.Error -> SlotUiState(error = state.message, retryAction = state.retryAction, data = oldData)
        is SlotState.Empty -> SlotUiState()
    }
    return SduiSlot(this.nodeId, uiState)
}

/**
 * Элегантный парсер для Вьюмоделей (для списков слотов).
 */
inline fun <reified T : SlotData> Map<SlotId, Map<String, SlotUpdate>>.extractSlots(
    id: SlotId,
    oldSlots: List<SduiSlot<T>> = emptyList()
): List<SduiSlot<T>> {
    return this[id]?.values?.map { update ->
        val oldSlot = oldSlots.find { it.nodeId == update.nodeId }
        val oldData = oldSlot?.state?.data
        update.toSduiSlot<T>(oldData)
    } ?: emptyList()
}

/**
 * Элегантный парсер для Вьюмоделей (для одиночного слота).
 */
inline fun <reified T : SlotData> Map<SlotId, Map<String, SlotUpdate>>.extractSlot(
    id: SlotId,
    oldSlot: SduiSlot<T>? = null
): SduiSlot<T>? {
    val update = this[id]?.values?.firstOrNull() ?: return null
    return update.toSduiSlot<T>(if (oldSlot?.nodeId == update.nodeId) oldSlot.state.data else null)
}
