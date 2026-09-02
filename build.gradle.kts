import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinx.rpc) apply false
    alias(libs.plugins.koin.compiler) apply false
}

plugins.withType<WasmYarnPlugin> {
    the<WasmYarnRootExtension>().apply {
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        yarnLockAutoReplace = true
    }
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().apply {
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        yarnLockAutoReplace = true
    }
}

tasks.register("compileAll") {
    group = "build"
    description = "Сборка бэкенда, общего клиента, веб-модуля, десктоп-модуля и плагинов"
    dependsOn(
        ":server:compileKotlin",
        ":client:compileKotlinWasmJs",
        ":client:compileKotlinJvm",
        ":desktopApp:compileKotlinJvm",
        ":web:compileDevelopmentExecutableKotlinWasmJs",
        ":basePlugins:home-feed-plugin:build",
        ":basePlugins:recommendation-plugin:build",
        ":basePlugins:media-details-plugin:build",
        ":basePlugins:person-details-plugin:build",
        ":basePlugins:media-list-plugin:build",
        ":basePlugins:trakt-metadata-plugin:build",
        ":basePlugins:torrserver-plugin:build",
        ":basePlugins:rutube-plugin:build",
        ":basePlugins:vk-video-plugin:build",
        ":basePlugins:anilibria-plugin:build"
    )
}

tasks.register("prepareDocker") {
    group = "docker"
    description = "Prepare distribution files for Docker build in build/docker-dist"
    dependsOn(
        ":server:buildFatJar",
        ":web:wasmJsBrowserDistribution",
        ":basePlugins:home-feed-plugin:build",
        ":basePlugins:recommendation-plugin:build",
        ":basePlugins:media-details-plugin:build",
        ":basePlugins:person-details-plugin:build",
        ":basePlugins:media-list-plugin:build",
        ":basePlugins:trakt-metadata-plugin:build",
        ":basePlugins:torrserver-plugin:build",
        ":basePlugins:rutube-plugin:build",
        ":basePlugins:vk-video-plugin:build",
        ":basePlugins:anilibria-plugin:build"
    )
    val rootProjectDir = layout.projectDirectory
    doLast {
        val dockerDistDir = rootProjectDir.dir("build/docker-dist").asFile
        dockerDistDir.mkdirs()
        val webTargetDir = File(dockerDistDir, "web").apply { mkdirs() }
        val pluginsTargetDir = File(dockerDistDir, "plugins").apply { mkdirs() }

        val serverJar = rootProjectDir.file("server/build/libs/avalon-media-card-server.jar").asFile
        if (serverJar.exists()) {
            serverJar.copyTo(File(dockerDistDir, "avalon-server.jar"), overwrite = true)
        }

        val webDist = rootProjectDir.dir("web/build/dist/wasmJs/productionExecutable").asFile
        if (webDist.exists()) {
            webDist.copyRecursively(webTargetDir, overwrite = true)
        }

        val pluginsDir = rootProjectDir.dir("server/plugins").asFile
        if (pluginsDir.exists()) {
            pluginsDir.listFiles { f -> f.extension == "jar" }?.forEach { jar ->
                jar.copyTo(File(pluginsTargetDir, jar.name), overwrite = true)
            }
        }
        println("✅ Docker distribution successfully prepared in build/docker-dist/")
    }
}

tasks.register("runDesktop") {
    group = "desktop"
    description = "Запуск десктопного приложения на ПК"
    dependsOn(":desktopApp:run")
}

tasks.register("packageLinuxDeb") {
    group = "desktop"
    description = "Сборка пакета Linux (.deb)"
    dependsOn(":desktopApp:packageDeb")
}

tasks.register("packageLinuxReleaseDeb") {
    group = "desktop"
    description = "Релизная сборка пакета Linux (.deb)"
    dependsOn(":desktopApp:packageReleaseDeb")
}

tasks.register("packageWindowsMsi") {
    group = "desktop"
    description = "Сборка установщика Windows (.msi)"
    dependsOn(":desktopApp:packageMsi")
}

tasks.register("packageWindowsExe") {
    group = "desktop"
    description = "Сборка установщика Windows (.exe)"
    dependsOn(":desktopApp:packageExe")
}

tasks.register("packageWindows") {
    group = "desktop"
    description = "Сборка всех дистрибутивов Windows (.msi и .exe)"
    dependsOn(":desktopApp:packageMsi", ":desktopApp:packageExe")
}

tasks.register("runAll") {
    group = "application"
    description = "Запуск бэкенда и фронтенда параллельно"
    dependsOn(
        ":basePlugins:home-feed-plugin:build",
        ":basePlugins:recommendation-plugin:build",
        ":basePlugins:media-details-plugin:build",
        ":basePlugins:person-details-plugin:build",
        ":basePlugins:media-list-plugin:build",
        ":basePlugins:trakt-metadata-plugin:build",
        ":basePlugins:torrserver-plugin:build",
        ":basePlugins:rutube-plugin:build",
        ":basePlugins:vk-video-plugin:build",
        ":basePlugins:anilibria-plugin:build"
    )

    val rootDirPath = rootDir.absolutePath

    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val execPath = if (isWindows) "gradlew.bat" else "./gradlew"
        val rootDirFile = File(rootDirPath)

        println("Запуск сервера...")
        val serverProcess = ProcessBuilder()
            .directory(rootDirFile)
            .command(execPath, ":server:run")
            .inheritIO()
            .start()

        println("Запуск веб-клиента...")
        val webProcess = ProcessBuilder()
            .directory(rootDirFile)
            .command(execPath, ":web:wasmJsBrowserDevelopmentRun")
            .inheritIO()
            .start()

        // Корректное завершение процессов при остановке задачи
        Runtime.getRuntime().addShutdownHook(Thread {
            serverProcess.destroy()
            webProcess.destroy()
        })

        serverProcess.waitFor()
        webProcess.waitFor()
    }
}
val downloadMpvTask = tasks.register("downloadMpv") {
    group = "build setup"
    description = "Скачивает и устанавливает libmpv-2.dll для работы видеоплеера на JVM (только Linux/macOS)"

    val clientDir = file("client/src/jvmMain/resources/win32-x86-64")
    val desktopDir = file("desktopApp/src/jvmMain/resources/win32-x86-64")
    val dllFilesExist = file("$clientDir/libmpv-2.dll").exists()

    doLast {
        val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
        if (isWindows) {
            if (!dllFilesExist) {
                logger.error(
                    """
                    
                    ========================================================================
                    [КРИТИЧЕСКАЯ ОШИБКА СБОРКИ] ПРИЯТЕЛЬ, ТЫ НА ВИНДЕ! 🚨
                    ========================================================================
                    Для сборки и запуска приложения под Windows тебе нужно ВРУЧНУЮ 
                    скачать библиотеку mpv-2.dll, иначе плеер работать не будет!
                    
                    ЧТО НУЖНО СДЕЛАТЬ:
                    1. Открой инструкцию: client/src/jvmMain/resources/win32-x86-64/README.md
                    2. Скачай архив mpv-dev-x86_64-*.7z по ссылкам из инструкции.
                    3. Достань оттуда libmpv-2.dll
                    4. Положи его в эти ДВЕ папки под именами mpv-2.dll И libmpv-2.dll:
                       - client/src/jvmMain/resources/win32-x86-64/
                       - desktopApp/src/jvmMain/resources/win32-x86-64/
                       
                    Как только файлы будут на месте, эта ошибка исчезнет.
                    ========================================================================
                    
                    """.trimIndent()
                )
                throw GradleException("Отсутствуют необходимые DLL файлы для Windows. См. лог выше.")
            }
        } else {
            if (!dllFilesExist) {
                println("[MPV] Скачивание mpv DLL через bash скрипт...")
                ProcessBuilder("bash", "scripts/download_mpv.sh").inheritIO().start().waitFor()
            } else {
                println("[MPV] Файлы DLL уже скачаны, пропускаем.")
            }
        }
    }
}

// Привязываем таску ко всем нужным модулям перед обработкой ресурсов
gradle.projectsEvaluated {
    project(":client").tasks.matching { it.name.contains("processResources") || it.name.contains("compileKotlinJvm") }.configureEach {
        dependsOn(downloadMpvTask)
    }
    project(":desktopApp").tasks.matching { it.name.contains("processResources") || it.name.contains("compileKotlinJvm") }.configureEach {
        dependsOn(downloadMpvTask)
    }
}
