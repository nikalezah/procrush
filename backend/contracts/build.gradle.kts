plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

kotlin.sourceSets.named("main") {
    kotlin.srcDir(rootProject.layout.projectDirectory.dir("i18n/generated/kotlin"))
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}
