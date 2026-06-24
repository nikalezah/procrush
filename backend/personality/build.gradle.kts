plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "jobs.procrush"
version = "1.0.0"

application {
    mainClass = "jobs.procrush.personality.app.PersonalityApplicationKt"
}

dependencies {
    implementation(projects.backend.wire)
    implementation(projects.backend.infra)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.client.cio)
    implementation(libs.amqp.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
