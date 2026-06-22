rootProject.name = "ProCrush"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
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

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

fun includeIfDir(path: String, dir: String) {
    if (file(dir).isDirectory) {
        include(path)
    }
}

includeIfDir(":app:androidApp", "app/androidApp")
includeIfDir(":app:desktopApp", "app/desktopApp")
includeIfDir(":app:shared", "app/shared")
includeIfDir(":app:webApp", "app/webApp")
include(":core")
include(":server")
include(":personality-worker")