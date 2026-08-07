plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

group = "jobs.procrush"
version = "1.0.0"

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "jobs.procrush.personality.app.main"
                baseName = "personality"
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(projects.backend.config)
            implementation(projects.backend.platform.rabbit)
            implementation(projects.backend.platform.llm)
            implementation(projects.backend.contracts)
            implementation(projects.backend.domain.personalityMessaging)
            implementation(libs.ktor.serverCore)
            implementation(libs.ktor.serverCio)
            implementation(libs.ktor.client.curl)
            implementation(libs.ktor.network)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
