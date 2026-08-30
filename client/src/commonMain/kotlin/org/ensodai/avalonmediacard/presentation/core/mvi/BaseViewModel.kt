package org.ensodai.avalonmediacard.presentation.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S : BaseViewState, A : BaseActions>(
    initialState: S
) : ViewModel() {

    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<S> = _viewState.asStateFlow()

    abstract val actions: A

    fun updateViewState(block: (S) -> S) {
        _viewState.update(block)
    }
}
