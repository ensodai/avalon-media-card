import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koin.compiler)
}

val composeResourcesSourceDir = layout.projectDirectory.dir(
    "../client/build/generated/assets/copyAndroidMainComposeResourcesToAndroidAssets"
)
val composeResourcesOutputRoot = layout.buildDirectory.dir("generated/composeAppAssets")

abstract class SyncComposeResourcesTask : Sync() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

fun registerComposeResourcesSyncTask(
    variantName: String,
) = tasks.register(
    "sync${variantName.replaceFirstChar(Char::uppercaseChar)}ComposeResources",
    SyncComposeResourcesTask::class
) {
    dependsOn(project(":client").tasks.named("copyAndroidMainComposeResourcesToAndroidAssets"))
    from(composeResourcesSourceDir)
    outputDir.convention(composeResourcesOutputRoot.map { it.dir(variantName) })
    into(outputDir)
}

val composeResourcesSyncTasks = mutableMapOf<String, TaskProvider<SyncComposeResourcesTask>>()

fun composeResourcesSyncTaskFor(buildType: String): TaskProvider<SyncComposeResourcesTask> =
    composeResourcesSyncTasks.getOrPut(buildType) {
        registerComposeResourcesSyncTask(buildType)
    }

fun getLocalIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (networkInterface in interfaces) {
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val name = networkInterface.name
            if (name.startsWith("docker") || name.startsWith("tun") || name.startsWith("virbr") || name.startsWith("br-") || name.startsWith("veth") || name.startsWith("tailscale")) {
                continue
            }
            for (address in networkInterface.inetAddresses) {
                if (address is Inet4Address) {
                    val ip = address.hostAddress
                    if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return "10.0.2.2"
}

android {
    namespace = "org.ensodai.avalonmediacard"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.ensodai.avalonmediacard"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            val localIp = getLocalIp()
            buildConfigField("String", "SERVER_URL", "\"ws://$localIp:8080/api/rpc\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            val prodServerUrl = project.findProperty("AVALON_PROD_SERVER_URL") as? String
                ?: System.getenv("AVALON_PROD_SERVER_URL")
                ?: ""
            buildConfigField("String", "SERVER_URL", "\"$prodServerUrl\"")
            val keystoreFile = project.findProperty("AVALON_KEYSTORE_FILE") as? String
                ?: System.getenv("AVALON_KEYSTORE_FILE")
            if (keystoreFile != null && file(keystoreFile).exists()) {
                val releaseSigning = signingConfigs.create("release") {
                    storeFile = file(keystoreFile)
                    storePassword = project.findProperty("AVALON_KEYSTORE_PASSWORD") as? String ?: System.getenv("AVALON_KEYSTORE_PASSWORD") ?: ""
                    keyAlias = project.findProperty("AVALON_KEY_ALIAS") as? String ?: System.getenv("AVALON_KEY_ALIAS") ?: ""
                    keyPassword = project.findProperty("AVALON_KEY_PASSWORD") as? String ?: System.getenv("AVALON_KEY_PASSWORD") ?: ""
                }
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            pickFirsts += "lib/**/libc++_shared.so"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: return@onVariants
        val syncTask = composeResourcesSyncTaskFor(buildType)
        variant.sources.assets?.addGeneratedSourceDirectory(
            syncTask,
            SyncComposeResourcesTask::outputDir
        )
    }
}

dependencies {
    implementation(project(":client"))
    implementation(libs.koinAndroid)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.mpv.lib)
    implementation(files("../libs/media3-decoder-ffmpeg-1.10.1-custom-v7.aar"))
    debugImplementation(libs.compose.uiTooling)
}
