import org.gradle.api.internal.StartParameterInternal
import org.gradle.initialization.StartParameterBuildOptions
import org.gradle.internal.buildoption.Option

rootProject.name = "procrush"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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

// Kind deploy tasks (and SpektorGenerateTask they pull in) are not configuration-cache
// compatible. Disable CC for those invocations so ./gradlew kindUp works.
val kindTaskNames = setOf(
    "kindUp",
    "kindDown",
    "frontendBuild",
    "generateI18n",
)
val requestedKindTasks = startParameter.taskNames.any { taskName ->
    taskName.substringAfterLast(':') in kindTaskNames
}
if (requestedKindTasks) {
    @Suppress("UnstableApiUsage")
    (startParameter as StartParameterInternal).apply {
        setConfigurationCache(Option.Value.value(false))
        // If CC is still enabled by gradle.properties timing, never fail the build for kind tasks.
        setConfigurationCacheProblems(StartParameterBuildOptions.ConfigurationCacheProblemsOption.Value.WARN)
        setConfigurationCacheQuiet(true)
    }
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
