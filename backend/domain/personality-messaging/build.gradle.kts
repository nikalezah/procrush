plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.backend.config)
            implementation(projects.backend.contracts)
            implementation(projects.backend.platform.rabbit)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
