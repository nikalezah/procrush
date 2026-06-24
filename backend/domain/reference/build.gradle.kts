plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.contracts)
    implementation(projects.backend.infra)
    implementation(projects.backend.schema)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
}
