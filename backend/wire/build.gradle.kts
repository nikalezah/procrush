plugins {
    alias(libs.plugins.kotlinJvm)
    `java-library`
}

group = "jobs.procrush"
version = "1.0.0"

dependencies {
    api(projects.backend.contracts)
    api(projects.backend.infra)
    api(projects.backend.domain.reference)
    api(projects.backend.domain.auth)
    api(projects.backend.domain.seeker)
    api(projects.backend.domain.employer)
    api(projects.backend.domain.survey)
    api(projects.backend.domain.matching)
    api(project(":backend:domain:personality-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.serverCore)
    implementation(libs.amqp.client)
    implementation(libs.kafka.clients)
}
