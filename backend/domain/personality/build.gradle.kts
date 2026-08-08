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
    implementation(projects.backend.platform.rabbit)
    implementation(projects.backend.platform.observability)
    implementation(projects.backend.domain.personalityMessaging)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.survey)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lettuce.core)
}
