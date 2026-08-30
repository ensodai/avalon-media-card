package org.ensodai.avalonmediacard.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun AvalonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // В десктопной версии нет системной кнопки "Назад",
    // поэтому перехват реализуется через клавиатуру (Esc)
    // на уровне окна или компонентов, что уже покрывается Compose Desktop.
}
