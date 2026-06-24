plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    implementation(projects.backend.contracts)
    implementation(libs.logback)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.lettuce.core)
    implementation(libs.amqp.client)
    implementation(libs.kafka.clients)
    implementation(projects.backend.schema)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.testcontainers.rabbitmq)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
