import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koin.compiler)
}

group = "org.ensodai.avalonmediacard"
version = providers.gradleProperty("web.versionName")
    .orElse(providers.gradleProperty("app.versionName"))
    .getOrElse("1.0.0")

kotlin {
    js {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
        commonMain.dependencies {
            implementation(projects.client)

            implementation(libs.compose.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }

        val jsMain by getting {
            dependencies {
                implementation(
                    npm(
                        "playsvideo",
                        "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.15.tgz"
                    )
                )
                implementation(
                    npm(
                        "mediabunny",
                        "file:${project.projectDir.parentFile.absolutePath}/libs/mediabunny-local.tgz"
                    )
                )
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(
                    npm(
                        "playsvideo",
                        "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.15.tgz"
                    )
                )
                implementation(
                    npm(
                        "mediabunny",
                        "file:${project.projectDir.parentFile.absolutePath}/libs/mediabunny-local.tgz"
                    )
                )
            }
        }
    }
}