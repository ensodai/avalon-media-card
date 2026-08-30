package org.ensodai.avalonmediacard

import android.util.Log
import org.ensodai.avalonmediacard.data.platformServerUrl

class AndroidApp : AvalonApplication() {
    override fun onCreate() {
        platformServerUrl = BuildConfig.SERVER_URL
        Log.d("AndroidApp", "SERVER_URL set to: $platformServerUrl")
        super.onCreate()
    }
}
