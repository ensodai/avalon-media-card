package org.ensodai.avalonmediacard.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * Мультиплатформенный перехватчик кнопки "Назад".
 */
@Composable
expect fun AvalonBackHandler(enabled: Boolean = true, onBack: () -> Unit)
