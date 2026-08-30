import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvm()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
        jvmMain.dependencies {
            implementation(projects.client)
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.logback)
            // JNA for native libmpv player (Windows DLLs: https://github.com/zhongfly/mpv-winbuild/releases)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.ensodai.avalonmediacard.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                TargetFormat.Deb,
                TargetFormat.Msi,
                TargetFormat.Exe
            )
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.sql",
                "java.xml",
                "java.management",
                "java.instrument",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.crypto.ec"
            )
            packageName = "AvalonMediaCard"
            packageVersion = "1.0.0"
            description = "Avalon Media Card Desktop"
            copyright = "© 2026 Ensodai"
            vendor = "Ensodai"

            linux {
                debPackageVersion = "1.0.0"
                menuGroup = "AudioVideo"
                appCategory = "AudioVideo"
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.png"))
            }

            windows {
                menuGroup = "Avalon Media Card"
                upgradeUuid = "6c810d29-a1b2-4d53-93fb-98cb9b6cb591"
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.ico"))
            }
        }
    }
}
