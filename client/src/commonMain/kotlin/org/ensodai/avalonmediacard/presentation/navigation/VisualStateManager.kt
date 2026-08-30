package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey

class VisualStateManager<T : NavKey> {
    val entries = mutableStateListOf<VisualEntry<T>>()

    private var zIndexCounter = 0f
    private var idCounter = 0L

    fun sync(logicalStack: List<T>) {
        val activeEntries = entries.toList()
        val newEntries = mutableListOf<VisualEntry<T>>()

        for (i in logicalStack.indices) {
            val route = logicalStack[i]

            val existing = activeEntries.find { it.route == route }

            if (existing != null) {
                newEntries.add(existing)
            } else {
                val newEntry = VisualEntry(
                    route = route,
                    id = "entry_${route.hashCode()}",
                    initialZIndex = ++zIndexCounter,
                    initialOffset = 0f
                )
                newEntry.state = VisualState.ACTIVE
                entries.add(newEntry)
                newEntries.add(newEntry)
            }
        }

        for (old in activeEntries) {
            if (!newEntries.contains(old)) {
                entries.remove(old)
                old.dispose()
            }
        }

        val lastIndex = newEntries.lastIndex
        for (i in newEntries.indices) {
            val entry = newEntries[i]
            if (i == lastIndex) {
                entry.state = VisualState.ACTIVE
            } else {
                entry.state = VisualState.BACKGROUND
            }
        }
    }
}
