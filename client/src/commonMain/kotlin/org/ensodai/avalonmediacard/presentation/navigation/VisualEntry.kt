package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.navigation3.runtime.NavKey

@Stable
class VisualEntry<T : NavKey>(
    val route: T,
    val id: String,
    initialZIndex: Float,
    initialOffset: Float = 0f
) : LifecycleOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private var _state by mutableStateOf(if (initialOffset == 0f) VisualState.ACTIVE else VisualState.ENTERING)
    var state: VisualState
        get() = _state
        set(value) {
            if (_state != value) {
                _state = value
                updateLifecycle(value)
            }
        }

    var zIndex by mutableFloatStateOf(initialZIndex)

    val offset = Animatable(initialOffset)

    var releaseDuration = AvalonTransitions.BASE_DURATION_MS

    init {
        updateLifecycle(_state)
    }

    private fun updateLifecycle(visualState: VisualState) {
        val targetLifecycle = when (visualState) {
            VisualState.ACTIVE -> Lifecycle.State.RESUMED
            VisualState.ENTERING,
            VisualState.UNDERNEATH,
            VisualState.EXITING -> Lifecycle.State.STARTED

            VisualState.BACKGROUND,
            VisualState.STASHED -> Lifecycle.State.CREATED
        }
        lifecycleRegistry.currentState = targetLifecycle
    }

    fun dispose() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
