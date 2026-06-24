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
    implementation(projects.backend.bootstrap)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverCallLogging)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverSse)
    implementation(libs.ktor.serialization.kotlinxJson)
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
