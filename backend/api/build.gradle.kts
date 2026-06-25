plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"
application {
    mainClass = "jobs.procrush.ApplicationKt"
}

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.platform.persistence)
    implementation(projects.backend.platform.redis)
    implementation(projects.backend.platform.rabbit)
    implementation(projects.backend.platform.kafka)
    implementation(projects.backend.contracts)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.employer)
    implementation(projects.backend.domain.survey)
    implementation(projects.backend.domain.matching)
    implementation(project(":backend:domain:personality-lib"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverCallLogging)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverSse)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kafka.clients)
    implementation(libs.amqp.client)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}
