package org.ensodai.avalonmediacard.presentation.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction

/**
 * Базовый контракт для состояния (State) любого SDUI-экрана.
 * Гарантирует, что каждый экран умеет обрабатывать и крутить лоадеры для серверных экшенов.
 */
interface SduiViewState {
    /**
     * Набор текущих выполняющихся серверных экшенов.
     * Используется для блокировки кнопок и показа спиннеров.
     */
    val loadingActions: Set<ServerAction>
}

/**
 * Универсальная базовая ViewModel для всех SDUI экранов.
 * 
 * Инкапсулирует работу с потоком состояния ([viewState]) и стандартизирует
 * обработку серверных действий через [executeServerAction].
 *
 * @param S Тип состояния, реализующий [SduiViewState].
 * @param initialState Начальное состояние экрана.
 */
abstract class SduiViewModel<S : SduiViewState>(
    initialState: S
) : ViewModel() {

    private val _viewState = MutableStateFlow(initialState)

    /**
     * Единственный источник истины для UI.
     * Собирается в Compose через `collectAsState()`.
     */
    val viewState: StateFlow<S> = _viewState.asStateFlow()

    /**
     * Безопасный метод для обновления состояния из дочерних ViewModel.
     * Заменяет прямые вызовы `_state.update { ... }`.
     */
    protected fun updateViewState(block: (S) -> S) {
        _viewState.update(block)
    }

    /**
     * Выполняет серверный экшен (например, сетевой запрос через SDUI контракт).
     * Результат работы этого метода будет перехвачен и обработан в [SduiCoordinator].
     *
     * @param action Действие, пришедшее от сервера, которое нужно выполнить.
     * @return Результат выполнения (Навигация, Ошибка, Пусто и т.д.).
     */
    abstract suspend fun executeServerAction(action: ServerAction): ActionResult

    /**
     * Перехватывает локальные UI экшены (ActionNavigate, ActionPlayVideo и др.).
     * По умолчанию ничего не делает. Переопределяется в конкретных ViewModel.
     */
    open fun handleLocalAction(action: org.ensodai.avalonmediacard.contract.slot.Action) {}
}
