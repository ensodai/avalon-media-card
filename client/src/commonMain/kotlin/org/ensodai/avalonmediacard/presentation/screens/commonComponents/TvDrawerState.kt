package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Внутренняя запись стека шторки.
 *
 * Поля объявлены как `State`, чтобы при обновлении параметров в [TvDrawerEffect] 
 * (например, изменился subtitle) перерисовывалась только нужная часть UI, 
 * а не весь контейнер шторки.
 */
class TvDrawerEntry(
    val id: String,
    initialTitle: String,
    initialSubtitle: String?,
    initialIcon: ImageVector?,
    initialOnDismiss: () -> Unit,
    initialContent: @Composable () -> Unit
) {
    var title by mutableStateOf(initialTitle)
    var subtitle by mutableStateOf(initialSubtitle)
    var icon by mutableStateOf(initialIcon)
    var onDismiss by mutableStateOf(initialOnDismiss)
    var content by mutableStateOf(initialContent)
}

/**
 * Состояние глобальной ТВ-шторки. Управляет стеком экранов.
 * 
 * ВАЖНО: Не вызывайте методы этого класса напрямую! 
 * Используйте декларативный [TvDrawerEffect] для безопасного добавления экранов в стек.
 */
@Stable
class TvDrawerState {
    var entries by mutableStateOf<List<TvDrawerEntry>>(emptyList())
        private set

    /** Открыта ли шторка (есть ли хотя бы один экран в стеке) */
    val isOpen: Boolean get() = entries.isNotEmpty()
    
    /** Текущий (верхний) экран в стеке */
    val current: TvDrawerEntry? get() = entries.lastOrNull()

    /**
     * Добавляет новый экран в стек или обновляет существующий по [id].
     * Метод внутренний, вызывается из [TvDrawerEffect].
     */
    internal fun addOrUpdate(
        id: String,
        title: String,
        subtitle: String?,
        icon: ImageVector?,
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val existing = entries.find { it.id == id }
        if (existing != null) {
            existing.title = title
            existing.subtitle = subtitle
            existing.icon = icon
            existing.onDismiss = onDismiss
            existing.content = content
        } else {
            entries = entries + TvDrawerEntry(id, title, subtitle, icon, onDismiss, content)
        }
    }

    /** Удаляет экран из стека. Вызывается при onDispose в [TvDrawerEffect]. */
    internal fun remove(id: String) {
        entries = entries.filter { it.id != id }
    }

    /** Программное закрытие всей шторки (полная очистка стека). */
    fun closeAll() {
        entries = emptyList()
    }

    /** 
     * Пользовательское закрытие (кнопка Back/Escape или клик по затемнению).
     * Закрывает только ВЕРХНИЙ экран, вызывая его колбэк [onDismiss].
     */
    fun dismissCurrent() {
        current?.onDismiss?.invoke()
    }
}

val LocalTvDrawerState = staticCompositionLocalOf { TvDrawerState() }

/**
 * Декларативный мост к глобальной ТВ-шторке со встроенной стековой навигацией.
 * 
 * **Как это работает:**
 * Пока этот composable находится в дереве, его контент отображается в шторке. 
 * Если вы рендерите несколько [TvDrawerEffect] одновременно (например, Главное меню и Озвучки), 
 * они автоматически выстраиваются в стек в порядке их рендеринга. 
 * Верхний экран плавно выезжает поверх нижнего.
 * 
 * **Восстановление фокуса (Stale Focus):**
 * Поскольку при открытии вложенных меню текущий экран НЕ уничтожается (остается в стеке), 
 * `LaunchedEffect(Unit)` сработает для него только один раз. 
 * Чтобы фокус возвращался корректно при закрытии вложенных меню, привязывайте фокус к стейту:
 * ```kotlin
 * LaunchedEffect(currentMenu) {
 *     if (currentMenu == MENU_MAIN) {
 *         focusRequester.requestFocus()
 *     }
 * }
 * ```
 */
@Composable
fun TvDrawerEffect(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = LocalTvDrawerState.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentContent by rememberUpdatedState(content)

    // Уникальный ID эффекта для идентификации его в стеке
    val effectId = remember { kotlin.random.Random.nextInt().toString() }

    DisposableEffect(effectId) {
        drawerState.addOrUpdate(
            id = effectId,
            title = title,
            subtitle = subtitle,
            icon = icon,
            onDismiss = { currentOnDismiss() },
            content = { currentContent() }
        )
        onDispose { drawerState.remove(effectId) }
    }

    SideEffect {
        drawerState.addOrUpdate(
            id = effectId,
            title = title,
            subtitle = subtitle,
            icon = icon,
            onDismiss = { currentOnDismiss() },
            content = { currentContent() }
        )
    }
}
