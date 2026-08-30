package org.ensodai.avalonmediacard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

private var androidContext: android.content.Context? = null

fun initAndroidContext(context: android.content.Context) {
    androidContext = context
}

actual fun createDataStore(): DataStore<Preferences> {
    val ctx = androidContext
        ?: throw IllegalStateException("Android Context must be initialized via initAndroidContext before createDataStore is called")
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { ctx.filesDir.resolve("avalon_settings.preferences_pb").absolutePath.toPath() }
    )
}
