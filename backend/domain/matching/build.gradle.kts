plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.contracts)
    implementation(projects.backend.platform.redis)
    implementation(projects.backend.platform.kafka)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.employer)
    implementation(projects.backend.domain.survey)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kafka.clients)
    implementation(libs.lettuce.core)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.junit)
    testImplementation(projects.backend.platform.redis)
    testImplementation(libs.testcontainers.redis)
}

tasks.test {
    useJUnitPlatform()
}
