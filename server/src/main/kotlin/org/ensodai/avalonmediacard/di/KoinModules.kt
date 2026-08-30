package org.ensodai.avalonmediacard.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import org.ensodai.avalonmediacard.security.SafeDnsResolver
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

@Module
@ComponentScan("org.ensodai.avalonmediacard")
class AppKoinModule {

    @Single
    fun httpClient(): HttpClient = HttpClient(OkHttp) {
        install(HttpCache)
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    @Single
    @Named("safeProxyHttpClient")
    fun safeProxyHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(false)
                followSslRedirects(false)
                connectionPool(ConnectionPool(200, 5, TimeUnit.MINUTES))
            }
        }
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = 60_000L
            connectTimeoutMillis = 15_000L
        }
    }
}

@KoinApplication(modules = [AppKoinModule::class])
object KtorKoinAppConfig
