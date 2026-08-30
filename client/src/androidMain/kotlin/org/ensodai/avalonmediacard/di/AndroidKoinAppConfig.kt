package org.ensodai.avalonmediacard.di

import org.ensodai.avalonmediacard.data.AppClientModule
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module(
    includes = [
        AppClientModule::class
    ]
)
class AndroidAppModule

@KoinApplication(modules = [AndroidAppModule::class])
object AndroidKoinAppConfig
