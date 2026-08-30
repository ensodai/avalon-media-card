plugins {
    alias(libs.plugins.kotlinJvm)
    kotlin("plugin.serialization") version "2.3.21"
}

dependencies {
    implementation("org.ensodai.avalonmediacard:avalon-media-card-core-contract:1.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(libs.kotlin.testJunit)
    testImplementation("io.ktor:ktor-client-mock:3.1.0")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("recommendation-plugin.jar")
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

tasks.test {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
