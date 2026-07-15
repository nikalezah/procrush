plugins {
    alias(libs.plugins.kotlinJvm)
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
    implementation(projects.backend.platform.persistence)
    implementation(projects.backend.platform.redis)
    implementation(projects.backend.platform.rabbit)
    implementation(projects.backend.platform.kafka)
    implementation(projects.backend.platform.llm)
    implementation(projects.backend.platform.observability)
    implementation(projects.backend.contracts)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.survey)
    implementation(projects.backend.domain.employer)
    implementation(projects.backend.domain.personalityLib)
    implementation(projects.backend.domain.matching)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.client.cio)
    implementation(libs.amqp.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kafka.clients)
}
