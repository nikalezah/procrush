plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.contracts)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
}
