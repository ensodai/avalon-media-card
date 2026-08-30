plugins {
    alias(libs.plugins.kotlinJvm)
    kotlin("plugin.serialization") version "2.3.21"
}

dependencies {
    implementation("org.ensodai.avalonmediacard:avalon-media-card-core-contract:1.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-core-jvm:3.5.0")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.5.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.5.0")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-cio-jvm:3.5.0")
    testImplementation("io.ktor:ktor-client-mock-jvm:3.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("lampac-adapter-plugin.jar")
}

val copyPluginJar = tasks.register<Copy>("copyPluginJar") {
    from(tasks.named("jar"))
    into(rootProject.file("server/plugins"))
}

tasks.named("build") {
    dependsOn(copyPluginJar)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}
