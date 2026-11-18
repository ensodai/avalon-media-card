import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone
import java.util.zip.GZIPOutputStream
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
                        "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.17.tgz"
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
                        "file:${project.projectDir.parentFile.absolutePath}/libs/playsvideo-0.4.7-f2.17.tgz"
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

val postProcessWasmDistribution by tasks.registering {
    dependsOn("wasmJsBrowserDistribution")
    val distDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    val buildHashProp = providers.gradleProperty("avalon.buildHash")
        .orElse(providers.environmentVariable("AVALON_BUILD_HASH"))
        .orElse(providers.provider {
            SimpleDateFormat("yyyyMMdd_HHmmss").apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        })
    val appVersionProp = providers.provider { project.version.toString() }

    // Динамическое получение версии протокола и ядра из core-contract
    val coreContractProps = providers.provider {
        val propsFile = project.rootProject.file("../avalon-media-card-core-contract/gradle.properties")
        val p = Properties()
        if (propsFile.exists()) {
            propsFile.inputStream().use { p.load(it) }
        }
        p
    }
    val protocolVersionProp = coreContractProps.map { p ->
        p.getProperty("core.protocolVersion") ?: "2.0"
    }
    val coreVersionProp = coreContractProps.map { p ->
        p.getProperty("core.version") ?: "1.2.1"
    }

    inputs.property("buildHash", buildHashProp)
    inputs.property("appVersion", appVersionProp)
    inputs.property("protocolVersion", protocolVersionProp)
    inputs.property("coreVersion", coreVersionProp)

    doLast {
        val dir = distDir.get().asFile
        if (!dir.exists()) return@doLast
        val buildHash = buildHashProp.get()
        val appVersion = appVersionProp.get()
        val protocolVersion = protocolVersionProp.get()
        val coreVersion = coreVersionProp.get()

        // 1. Создаем version.json с реальными версиями контракта и протокола
        val versionJsonFile = dir.resolve("version.json")
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val manifest = """
        {
          "buildHash": "$buildHash",
          "appVersion": "$appVersion",
          "coreVersion": "$coreVersion",
          "protocolVersion": "$protocolVersion",
          "builtAt": "$isoDate"
        }
        """.trimIndent()
        versionJsonFile.writeText(manifest)

        // 2. Инъекция buildHash и Cache-Busting параметров в index.html
        val htmlFile = dir.resolve("index.html")
        if (htmlFile.exists()) {
            var html = htmlFile.readText()
            val metaTag = """<meta name="avalon-build-hash" content="$buildHash">"""
            if (!html.contains("avalon-build-hash")) {
                html = html.replace("<head>", "<head>\n    $metaTag")
            } else {
                html = html.replace(Regex("""<meta\s+name="avalon-build-hash"\s+content="[^"]*">"""), metaTag)
            }
            html = html.replace("src=\"web.js\"", "src=\"web.js?v=$buildHash\"")
            html = html.replace("href=\"styles.css\"", "href=\"styles.css?v=$buildHash\"")
            htmlFile.writeText(html)
        }

        // 3. Предсжатие gzip sidecar файлов (.gz) для nginx gzip_static
        dir.walkTopDown()
            .filter { it.isFile && it.extension in setOf("wasm", "js", "mjs", "css", "html", "json") && !it.name.endsWith(".gz") }
            .forEach { asset ->
                val gzFile = File(asset.parentFile, "${asset.name}.gz")
                GZIPOutputStream(gzFile.outputStream()).use { gz ->
                    asset.inputStream().use { input -> input.copyTo(gz) }
                }
            }
    }
}

tasks.named("wasmJsBrowserDistribution") {
    finalizedBy(postProcessWasmDistribution)
}
