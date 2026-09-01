import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.rpc)
    alias(libs.plugins.koin.compiler)
    kotlin("plugin.serialization") version "2.3.21"
}

group = "org.ensodai.avalonmediacard"
version = providers.gradleProperty("server.versionName")
    .orElse(providers.gradleProperty("app.versionName"))
    .getOrElse("1.0.0")
application {
    mainClass = "org.ensodai.avalonmediacard.ApplicationKt"
}

koinCompiler {
    userLogs = true
    debugLogs = false
    unsafeDslChecks = false
}

dependencies {
    implementation(libs.avalon.core.contract)
    implementation(libs.java.jwt)
    implementation(libs.trakt.api)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.8.0")
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverCompression)
    implementation(libs.ktor.serverForwardedHeader)
    implementation("io.ktor:ktor-server-cors-jvm:3.5.0")
    implementation("io.ktor:ktor-server-websockets-jvm:3.5.0")
    implementation("io.ktor:ktor-server-partial-content-jvm:3.5.0")
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)

    implementation("io.ktor:ktor-client-core-jvm:3.5.0")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.5.0")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.5.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.5.0")
    implementation("io.ktor:ktor-client-auth-jvm:3.5.0")

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.annotations)

    // Database: Exposed, SQLite, HikariCP, Flyway
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.kotlinx.datetime)
    implementation(libs.hikaricp)
    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

ktor {
    fatJar {
        archiveFileName.set("avalon-media-card-server.jar")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("avalon-media-card-server.jar")
    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}