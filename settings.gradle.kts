rootProject.name = "Avalonmediacard"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

if (file("../avalon-media-card-core-contract").exists()) {
    includeBuild("../avalon-media-card-core-contract")
} else if (file("avalon-media-card-core-contract").exists()) {
    includeBuild("avalon-media-card-core-contract")
}

pluginManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/ensodai/avalon-media-card-core-contract")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "ensodai"
                password = System.getenv("GITHUB_TOKEN") ?: System.getenv("SERVER_DEPLOY_KEY")
            }
        }
    }
}

include(":client")
include(":desktopApp")
include(":androidApp")
include(":web")
include(":server")
include(":basePlugins:home-feed-plugin")
include(":basePlugins:media-details-plugin")
include(":basePlugins:person-details-plugin")
include(":basePlugins:media-list-plugin")
include(":basePlugins:trakt-metadata-plugin")
include(":basePlugins:torrserver-plugin")
include(":basePlugins:recommendation-plugin")
include(":basePlugins:anilibria-plugin")
include(":basePlugins:rutube-plugin")
include(":basePlugins:collaps-plugin")
include(":basePlugins:vk-video-plugin")
include(":basePlugins:lampac-adapter-plugin")