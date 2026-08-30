package org.ensodai.avalonmediacard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine

enum class UiModeOverride {
    AUTO, TV, PC
}

class AppSettingsStorage(private val dataStore: DataStore<Preferences>) {
    private val defaultPlayerKey = stringPreferencesKey("default_player_engine")
    private val uiModeOverrideKey = stringPreferencesKey("ui_mode_override")
    private val appLanguageKey = stringPreferencesKey("app_language")

    private val _language = MutableStateFlow<String>("auto")
    val language = _language.asStateFlow()

    var cachedLanguage: String = "auto"
        private set

    private val _defaultPlayer = MutableStateFlow<PlayerEngine>(PlayerEngine.MEDIA3)
    val defaultPlayer = _defaultPlayer.asStateFlow()

    var cachedDefaultPlayer: PlayerEngine = PlayerEngine.MEDIA3
        private set

    private val _uiModeOverride = MutableStateFlow<UiModeOverride>(UiModeOverride.AUTO)
    val uiModeOverride = _uiModeOverride.asStateFlow()

    var cachedUiModeOverride: UiModeOverride = UiModeOverride.AUTO
        private set

    private val _settingsVersion = MutableStateFlow<Long>(0L)
    val settingsVersion = _settingsVersion.asStateFlow()

    fun notifySettingsChanged() {
        _settingsVersion.value += 1L
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            dataStore.data.collect { prefs ->
                val playerStr = prefs[defaultPlayerKey] ?: "MEDIA3"
                val player = runCatching { PlayerEngine.valueOf(playerStr) }.getOrDefault(PlayerEngine.MEDIA3)
                cachedDefaultPlayer = player
                _defaultPlayer.value = player

                val modeStr = prefs[uiModeOverrideKey] ?: "AUTO"
                val mode = runCatching { UiModeOverride.valueOf(modeStr) }.getOrDefault(UiModeOverride.AUTO)
                cachedUiModeOverride = mode
                _uiModeOverride.value = mode

                val langCode = prefs[appLanguageKey] ?: "auto"
                cachedLanguage = langCode
                _language.value = langCode
            }
        }
    }

    suspend fun saveDefaultPlayer(playerEngine: PlayerEngine) {
        cachedDefaultPlayer = playerEngine
        dataStore.edit {
            it[defaultPlayerKey] = playerEngine.name
        }
    }

    suspend fun saveUiModeOverride(mode: UiModeOverride) {
        cachedUiModeOverride = mode
        dataStore.edit {
            it[uiModeOverrideKey] = mode.name
        }
    }

    suspend fun saveLanguage(languageCode: String) {
        cachedLanguage = languageCode
        dataStore.edit {
            it[appLanguageKey] = languageCode
        }
    }
}
