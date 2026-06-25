plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "jobs.procrush"
version = "1.0.0"

application {
    mainClass = "jobs.procrush.matching.runtime.MatchingApplicationKt"
    applicationName = "matching"
}

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.platform.redis)
    implementation(projects.backend.platform.kafka)
    implementation(projects.backend.contracts)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kafka.clients)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
}
