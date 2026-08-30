package org.ensodai.avalonmediacard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences> {
    val dir = getAppDataDirectory()
    val file = File(dir, "avalon_settings.preferences_pb")
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
}

private fun getAppDataDirectory(): File {
    val os = System.getProperty("os.name", "").lowercase()
    val dir = when {
        os.contains("win") -> {
            val appData = System.getenv("APPDATA")
            if (!appData.isNullOrBlank()) File(appData, "AvalonMediaCard")
            else File(System.getProperty("user.home"), ".avalonmediacard")
        }
        os.contains("mac") -> {
            File(System.getProperty("user.home"), "Library/Application Support/AvalonMediaCard")
        }
        else -> {
            val configHome = System.getenv("XDG_CONFIG_HOME")
            if (!configHome.isNullOrBlank()) File(configHome, "avalonmediacard")
            else File(System.getProperty("user.home"), ".config/avalonmediacard")
        }
    }
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}
