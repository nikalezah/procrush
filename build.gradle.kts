plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

fun nodeAvailable(): Boolean =
    try {
        val nodeCmd =
            if (System.getProperty("os.name").lowercase().contains("windows")) "node.exe" else "node"
        ProcessBuilder(nodeCmd, "--version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }

tasks.register<Exec>("generateI18n") {
    group = "i18n"
    description = "Generate ErrorCode from i18n/error-codes.yaml (requires Node.js)"
    workingDir = layout.projectDirectory.dir("i18n").asFile
    val nodeCmd =
        if (System.getProperty("os.name").lowercase().contains("windows")) "node.exe" else "node"
    commandLine(nodeCmd, "scripts/generate.mjs")
    notCompatibleWithConfigurationCache("uses script-local onlyIf / Node detection")

    val generatedKotlin =
        layout.projectDirectory.file("i18n/generated/kotlin/jobs/procrush/i18n/ErrorCode.kt")
    val generatedTs =
        layout.projectDirectory.file("i18n/generated/typescript/errorCodes.ts")

    // Prod Docker images (JDK only) have no Node; generated sources are committed in the repo.
    onlyIf {
        if (nodeAvailable()) {
            true
        } else if (generatedKotlin.asFile.exists() && generatedTs.asFile.exists()) {
            logger.lifecycle(
                "Skipping generateI18n: Node.js not available; using committed generated sources",
            )
            false
        } else {
            throw org.gradle.api.GradleException(
                "generateI18n requires Node.js, and i18n/generated sources are missing",
            )
        }
    }
}

apply(from = "gradle/kind-deploy.gradle.kts")
