import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spektor)
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

// Spektor emits a leading "_" in package segments on Windows only; normalize for cross-platform builds (Docker/kind).
tasks.named("spektorGenerate") {
    doLast {
        normalizeSpektorPackages(spektorGeneratedDir.get().asFile)
    }
}

fun normalizeSpektorPackages(root: File) {
    if (!root.exists()) return
    root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
        val text = file.readText()
        val fixed = text.replace("jobs.procrush.api.generated._", "jobs.procrush.api.generated.")
        if (text != fixed) file.writeText(fixed)
    }
    root.walkTopDown()
        .filter { it.isDirectory && it.name.startsWith("_") }
        .toList()
        .sortedByDescending { it.path.length }
        .forEach { dir ->
            val target = File(dir.parentFile, dir.name.removePrefix("_"))
            if (target.exists()) {
                check(target.deleteRecursively()) { "Failed to delete ${target.path}" }
            }
            check(dir.renameTo(target)) { "Failed to rename ${dir.path} -> ${target.path}" }
        }
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
}
