package org.ensodai.avalonmediacard.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DialogManager {
    private val _activeDialog = MutableStateFlow<String?>(null)
    val activeDialog: StateFlow<String?> = _activeDialog.asStateFlow()

    fun show(dialog: String) {
        _activeDialog.value = dialog
    }

    fun dismiss() {
        _activeDialog.value = null
    }
}
