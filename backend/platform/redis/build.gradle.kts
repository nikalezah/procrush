plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.config)
    implementation(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
}

tasks.test {
    useJUnitPlatform()
}
