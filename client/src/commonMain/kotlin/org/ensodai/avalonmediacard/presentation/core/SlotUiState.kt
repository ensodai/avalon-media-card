package org.ensodai.avalonmediacard.presentation.core

import org.ensodai.avalonmediacard.contract.slot.Action

data class SlotUiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val retryAction: Action? = null
) {
    val isInitialLoading: Boolean get() = data == null && isLoading
    val isRefreshing: Boolean get() = data != null && isLoading
    val isEmpty: Boolean get() = data == null && !isLoading && error == null
    val hasError: Boolean get() = error != null
}
