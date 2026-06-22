plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "jobs.procrush"
version = "1.0.0"

application {
    mainClass = "jobs.procrush.worker.PersonalityWorkerApplicationKt"
}

dependencies {
    implementation(project(":server"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
}
