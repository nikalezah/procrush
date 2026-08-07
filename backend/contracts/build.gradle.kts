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
        commonMain {
            kotlin.srcDir(rootProject.layout.projectDirectory.dir("i18n/generated/kotlin"))
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}

tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(":generateI18n")
}
