package org.ensodai.avalonmediacard.desktop.di

import org.ensodai.avalonmediacard.data.AppClientModule
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module(
    includes = [
        AppClientModule::class
    ]
)
class DesktopAppModule

@KoinApplication(modules = [DesktopAppModule::class])
object DesktopKoinAppConfig
