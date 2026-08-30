package org.ensodai.avalonmediacard.plugin

import kotlinx.coroutines.flow.*
import org.ensodai.avalonmediacard.contract.plugins.PluginSettings
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository

class PluginSettingsImpl(
    private val pluginId: String,
    private val repository: SystemSettingsRepository,
    private val changeEvents: MutableSharedFlow<String>
) : PluginSettings {

    private fun getPrefixedKey(key: String): String = "plugin:$pluginId:$key"

    override suspend fun getString(key: String): String? {
        return repository.getSetting(getPrefixedKey(key))
    }

    override suspend fun setString(key: String, value: String) {
        val prefixedKey = getPrefixedKey(key)
        repository.saveSetting(prefixedKey, value)
        changeEvents.emit(prefixedKey)
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getString(key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        setString(key, value.toString())
    }

    override fun observeString(key: String, defaultValue: String?): Flow<String?> {
        val prefixedKey = getPrefixedKey(key)
        return changeEvents
            .filter { it == prefixedKey }
            .map { getString(key) }
            .onStart { emit(getString(key) ?: defaultValue) }
            .distinctUntilChanged()
    }

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> {
        val prefixedKey = getPrefixedKey(key)
        return changeEvents
            .filter { it == prefixedKey }
            .map { getBoolean(key, defaultValue) }
            .onStart { emit(getBoolean(key, defaultValue)) }
            .distinctUntilChanged()
    }
}
