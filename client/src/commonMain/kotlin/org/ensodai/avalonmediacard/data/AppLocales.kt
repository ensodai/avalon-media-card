package org.ensodai.avalonmediacard.data

data class LanguageDescriptor(
    val code: String,        // "auto", "ru", "en", "original", etc.
    val displayName: String, // Fallback name
    val nativeName: String   // Native language name
)

object AppLocales {
    val supported = listOf(
        LanguageDescriptor("auto", "Auto", "System"),
        LanguageDescriptor("ru", "Русский", "Русский"),
        LanguageDescriptor("en", "English", "English")
    )
}
