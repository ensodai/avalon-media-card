package org.ensodai.avalonmediacard.data


import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer

actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        storage = WebLocalStorage(
            serializer = PreferencesSerializer,
            name = "avalon_settings"
        )
    )
}
