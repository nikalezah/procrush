plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.config)
    implementation(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.core)
}
