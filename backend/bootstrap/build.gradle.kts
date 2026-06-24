plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    `java-library`
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    api(projects.backend.wire)
    implementation(libs.kotlinx.coroutines.core)
}
