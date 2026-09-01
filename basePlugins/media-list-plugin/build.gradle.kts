plugins {
    alias(libs.plugins.kotlinJvm)
    kotlin("plugin.serialization") version "2.3.21"
}

dependencies {
    implementation(libs.avalon.core.contract)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("media-list-plugin.jar")
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
