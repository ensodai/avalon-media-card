package org.ensodai.avalonmediacard.di

import org.ensodai.avalonmediacard.data.AppClientModule
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module(
    includes = [
        AppClientModule::class
    ]
)
class WebAppModule

@KoinApplication(modules = [WebAppModule::class])
object WebKoinAppConfig
