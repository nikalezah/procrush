plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "jobs.procrush"
version = "1.0.0"

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            // Port types only — no AMQP transport dependency.
        }
    }
}
