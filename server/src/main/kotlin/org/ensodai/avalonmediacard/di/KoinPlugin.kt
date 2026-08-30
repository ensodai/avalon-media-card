package org.ensodai.avalonmediacard.di

import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.koinPlugin() {
    install(Koin) {
        modules(
            AppKoinModule().module()
        )
    }
}
