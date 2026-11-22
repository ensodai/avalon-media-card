import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "org.ensodai.avalonmediacard"
version = providers.gradleProperty("app.versionName").getOrElse("1.0.0")

kotlin {
    android {
        namespace = "org.ensodai.avalonmediacard.designsystem"
        compileSdk = (project.findProperty("android.compileSdk") ?: project.findProperty("app.compileSdk") ?: "37").toString().toInt()
        minSdk = (project.findProperty("android.minSdk") ?: project.findProperty("app.minSdk") ?: "26").toString().toInt()
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
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.lucide.cmp)
        }
    }
}
