package org.ensodai.avalonmediacard.presentation.locale

import java.util.Locale

private val defaultSystemLocale: Locale = Locale.getDefault()

actual fun setAppLocale(language: String) {
    val targetLocale = if (language.equals("auto", ignoreCase = true)) {
        defaultSystemLocale
    } else {
        Locale.forLanguageTag(language)
    }
    Locale.setDefault(targetLocale)
}
