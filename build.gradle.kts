plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

tasks.register<Exec>("generateI18n") {
    group = "i18n"
    description = "Generate ErrorCode from i18n/error-codes.yaml (requires Node.js)"
    workingDir = layout.projectDirectory.dir("i18n").asFile
    commandLine("node", "scripts/generate.mjs")
}
