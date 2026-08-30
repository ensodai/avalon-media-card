package org.ensodai.avalonmediacard.plugin.recommendation

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.i18n.MapPluginI18n
import org.ensodai.avalonmediacard.contract.i18n.PluginI18n

object RecommendationI18nHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cached: PluginI18n by lazy {
        val translations = mutableMapOf<String, Map<String, String>>()
        val classLoader = RecommendationI18nHelper::class.java.classLoader
        for (lang in listOf("ru", "en")) {
            try {
                val text = classLoader.getResourceAsStream("i18n/$lang.json")?.bufferedReader()?.readText()
                if (text != null) {
                    translations[lang] = json.decodeFromString<Map<String, String>>(text)
                }
            } catch (e: Exception) {
                // Ignore missing locale resources
            }
        }
        MapPluginI18n(translations, defaultLocale = "en")
    }

    fun load(): PluginI18n = cached
}
