plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation("org.ensodai.avalonmediacard:avalon-media-card-core-contract:1.0.0-SNAPSHOT")
}

// Таск для автоматического копирования JAR в папку plugins сервера бэкенда
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
