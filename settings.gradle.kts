rootProject.name = "procrush"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":backend:contracts")
include(":backend:config")
include(":backend:platform:redis")
include(":backend:platform:rabbit")
include(":backend:platform:kafka")
include(":backend:platform:llm")
include(":backend:platform:observability")
include(":backend:platform:persistence")
include(":backend:domain:reference")
include(":backend:domain:auth")
include(":backend:domain:seeker")
include(":backend:domain:employer")
include(":backend:domain:survey")
include(":backend:domain:matching")
include(":backend:domain:personality-lib")
project(":backend:domain:personality-lib").projectDir = file("backend/domain/personality")
include(":backend:api")
include(":backend:matching")
include(":backend:personality")
