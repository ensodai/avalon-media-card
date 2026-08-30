package org.ensodai.avalonmediacard

import android.app.Application
import org.ensodai.avalonmediacard.data.initAndroidContext
import org.ensodai.avalonmediacard.di.AndroidKoinAppConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

open class AvalonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidContext(this)
        startKoin<AndroidKoinAppConfig> {
            androidLogger()
            androidContext(this@AvalonApplication)
        }
    }
}
