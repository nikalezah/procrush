plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.contracts)
    implementation(project(":backend:domain:reference"))
    implementation(project(":backend:domain:auth"))
    implementation(project(":backend:domain:seeker"))
    implementation(project(":backend:domain:employer"))
    implementation(project(":backend:domain:survey"))
    implementation(project(":backend:domain:matching"))
    implementation(project(":backend:domain:personality-lib"))
    implementation(libs.logback)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.kotlinx.serialization.json)
}
