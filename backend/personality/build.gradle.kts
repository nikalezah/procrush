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
    implementation(projects.backend.contracts)
    implementation(project(":backend:domain:reference"))
    implementation(project(":backend:domain:auth"))
    implementation(project(":backend:domain:seeker"))
    implementation(project(":backend:domain:survey"))
    implementation(project(":backend:domain:employer"))
    implementation(project(":backend:domain:personality-lib"))
    implementation(project(":backend:domain:matching"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.client.cio)
    implementation(libs.amqp.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kafka.clients)
}
