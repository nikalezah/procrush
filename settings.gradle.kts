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
include(":backend:contracts")
include(":backend:schema")
include(":backend:infra")
include(":backend:domain:reference")
include(":backend:domain:auth")
include(":backend:domain:seeker")
include(":backend:domain:employer")
include(":backend:domain:survey")
include(":backend:domain:matching")
include(":backend:domain:personality-core")
project(":backend:domain:personality-core").projectDir = file("backend/domain/personality")
include(":backend:bootstrap")
include(":backend:wire")
include(":backend:api")
include(":backend:matching")
include(":backend:personality")