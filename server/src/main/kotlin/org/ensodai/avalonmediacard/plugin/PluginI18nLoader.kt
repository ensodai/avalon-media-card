package org.ensodai.avalonmediacard.plugin

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.i18n.EmptyPluginI18n
import org.ensodai.avalonmediacard.contract.i18n.MapPluginI18n
import org.ensodai.avalonmediacard.contract.i18n.PluginI18n
import java.io.File
import java.util.jar.JarFile

object PluginI18nLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val commonLocaleCodes = listOf("en", "ru", "de", "es", "fr", "it", "zh", "ja", "uk", "pl", "pt", "tr")

    fun loadI18n(classLoader: ClassLoader, jarFile: File? = null): PluginI18n {
        val translations = mutableMapOf<String, MutableMap<String, String>>()

        // 1. If jarFile exists, inspect all entries in i18n/ folder
        if (jarFile != null && jarFile.exists() && jarFile.isFile) {
            try {
                JarFile(jarFile).use { jar ->
                    val entries = jar.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name.startsWith("i18n/") && name.endsWith(".json")) {
                            val locale = name.removePrefix("i18n/").removeSuffix(".json").lowercase()
                            jar.getInputStream(entry).use { stream ->
                                val text = stream.bufferedReader().readText()
                                parseAndMerge(translations, locale, text)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to read i18n from jar: ${jarFile.name} (${e.message})")
            }
        }

        // 2. ClassLoader resource lookup (for tests, embedded plugins, or fallback)
        for (lang in commonLocaleCodes) {
            val resourcePath = "i18n/$lang.json"
            try {
                classLoader.getResourceAsStream(resourcePath)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    parseAndMerge(translations, lang, text)
                }
            } catch (e: Exception) {
                // Ignore missing locale resources
            }
        }

        if (translations.isEmpty()) {
            return EmptyPluginI18n
        }

        return MapPluginI18n(translations, defaultLocale = "en")
    }

    private fun parseAndMerge(
        target: MutableMap<String, MutableMap<String, String>>,
        locale: String,
        jsonContent: String
    ) {
        try {
            val map = json.decodeFromString<Map<String, String>>(jsonContent)
            val localeMap = target.getOrPut(locale) { mutableMapOf() }
            localeMap.putAll(map)
        } catch (e: Exception) {
            System.err.println("Failed to parse i18n JSON for locale '$locale': ${e.message}")
        }
    }
}
