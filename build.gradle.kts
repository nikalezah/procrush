plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            add("testRuntimeOnly", rootProject.libs.junit.platform.launcher)
        }
    }
}