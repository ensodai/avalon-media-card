package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class AvalonNavController<T : NavKey>(
    val backStack: NavBackStack<T>
) {
    val canPop: Boolean get() = backStack.size > 1

    fun navigate(route: T) {
        println("AvalonNavController: navigate to screen = $route, from screen = ${backStack.lastOrNull()}")
        if (backStack.isNotEmpty() && backStack.last() == route) return
        backStack.add(route)
    }

    fun pop() {
        println("AvalonNavController: pop")
        if (canPop) backStack.removeAt(backStack.size - 1)
    }

    fun popToRoot() {
        println("AvalonNavController: popToRoot (current stack size = ${backStack.size})")
        while (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun clearAndNavigate(route: T) {
        println("AvalonNavController: clear and navigate to screen = $route")
        while (backStack.size > 0) {
            backStack.removeAt(backStack.size - 1)
        }
        backStack.add(route)
    }
}

@Composable
fun <T : NavKey> rememberAvalonNavController(
    backStack: NavBackStack<T>
): AvalonNavController<T> {
    return remember(backStack) { AvalonNavController(backStack) }
}
