import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spektor)
    id("procrush.api")
}

spektor {
    specRoot = rootProject.layout.projectDirectory.file("openapi/specs").asFile
    basePackage = "jobs.procrush.api.generated"
    dtoSuffix = ""
    serverApiSuffix = "ServerApi"
    routesSuffix = "Routes"
}

val spektorGeneratedDir = layout.buildDirectory.dir("spektor-generated/kotlin")

kotlin.sourceSets.named("main") {
    kotlin.srcDir(spektorGeneratedDir)
}

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn("spektorGenerate")
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
    implementation(projects.backend.platform.observability)
    implementation(projects.backend.contracts)
    implementation(projects.backend.domain.reference)
    implementation(projects.backend.domain.auth)
    implementation(projects.backend.domain.seeker)
    implementation(projects.backend.domain.employer)
    implementation(projects.backend.domain.survey)
    implementation(projects.backend.domain.matching)
    implementation(projects.backend.domain.personalityLib)
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
}
