import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.rpc)
    alias(libs.plugins.koin.compiler)
    kotlin("plugin.serialization") version "2.3.21"
}

kotlin {
    android {
        namespace = "org.ensodai.avalonmediacard.client"
        compileSdk = 37
        minSdk = 26
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }


    sourceSets {
        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
        commonMain.dependencies {
            api("org.ensodai.avalonmediacard:avalon-media-card-core-contract:1.0.0-SNAPSHOT")
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.okio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.lucide.cmp)

            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlinx.rpc.krpc.client)
            implementation(libs.kotlinx.rpc.krpc.ktor.client)
            implementation(libs.kotlinx.rpc.krpc.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.compose.uiTooling)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.exoplayer.dash)
            implementation(libs.media3.ui)
            implementation(libs.media3.datasource.okhttp)
            implementation(libs.media3.extractor)
            implementation(libs.media3.common)
            compileOnly(files("../libs/media3-decoder-ffmpeg-1.10.1-custom-v7.aar"))
            implementation(libs.media3.avi)
            implementation(libs.mpv.lib)
            implementation(libs.tv.material3)
            implementation(libs.compose.uiTooling)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(
                npm(
                    "playsvideo",
                    "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.15.tgz"
                )
            )
            implementation(npm("hls.js", "^1.5.0"))
            implementation(npm("dashjs", "^5.2.1"))
            implementation(npm("mpegts.js", "^1.8.0"))
            implementation(npm("libav.js", "*"))
            implementation(npm("libavjs-webcodecs-polyfill", "*"))
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(
                    npm(
                        "playsvideo",
                        "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.15.tgz"
                    )
                )
                implementation(npm("hls.js", "^1.5.0"))
                implementation(npm("dashjs", "^5.2.1"))
                implementation(npm("mpegts.js", "^1.8.0"))
                implementation(npm("libav.js", "*"))
                implementation(npm("libavjs-webcodecs-polyfill", "*"))
            }
        }
    }
}

koinCompiler {
    userLogs = true
    strictSafety = true
}

compose.resources {
    packageOfResClass = "avalonmediacard.client.generated.resources"
    generateResClass = always
    publicResClass = true
}

tasks.matching { it.name == "copyAndroidMainComposeResourcesToAndroidAssets" }.configureEach {
    val outputDirectory = javaClass.methods
        .firstOrNull { method -> method.name == "getOutputDirectory" }
        ?.invoke(this) as? DirectoryProperty

    outputDirectory?.convention(
        layout.buildDirectory.dir("generated/assets/$name")
    )
}