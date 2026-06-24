plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.contracts)
    implementation(projects.backend.infra)
    implementation(projects.backend.schema)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.employer)
    implementation(projects.backend.domain.survey)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kafka.clients)
    implementation(libs.lettuce.core)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
