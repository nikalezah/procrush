plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "jobs.procrush"
version = "1.0.0"

application {
    mainClass = "jobs.procrush.personality.app.PersonalityApplicationKt"
    applicationName = "personality"
}

dependencies {
    implementation(projects.backend.config)
    implementation(projects.backend.platform.rabbit)
    implementation(projects.backend.platform.llm)
    implementation(projects.backend.contracts)
    implementation(projects.backend.domain.personalityMessaging)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverCio)
    implementation(libs.ktor.client.cio)
    implementation(libs.amqp.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}
