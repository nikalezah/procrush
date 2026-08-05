package jobs.procrush.gradle

import jobs.procrush.gradle.spektor.NormalizeSpektorPackagesTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProcrushApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val generatedDir = target.layout.buildDirectory.dir("spektor-generated/kotlin")
        val normalizedDir = target.layout.buildDirectory.dir("spektor-normalized/kotlin")

        val normalize = target.tasks.register(
            "normalizeSpektorPackages",
            NormalizeSpektorPackagesTask::class.java,
        ) {
            sourceDir.set(generatedDir)
            outputDir.set(normalizedDir)
        }

        target.pluginManager.withPlugin("io.github.vooft.spektor") {
            normalize.configure {
                dependsOn("spektorGenerate")
            }
            target.tasks.named("spektorGenerate") {
                finalizedBy(normalize)
            }
            target.tasks.named("compileKotlin") {
                dependsOn(normalize)
            }
            target.extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
                sourceSets.named("main") {
                    kotlin.srcDir(normalizedDir)
                }
            }
            // The Spektor plugin registers its raw output (`spektor-generated`) as a
            // Kotlin source directory. We compile the normalized copy instead. On
            // Windows the raw packages carry a leading underscore, so the two trees
            // differ; on Linux they are identical and compiling both yields
            // "Redeclaration" errors. Drop the raw tree so only the normalized copy
            // is compiled on every platform.
            target.afterEvaluate {
                target.extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
                    sourceSets.named("main") {
                        val kept =
                            kotlin.srcDirs.filterNot { dir ->
                                dir.path.replace('\\', '/').contains("/spektor-generated")
                            }
                        kotlin.setSrcDirs(kept)
                    }
                }
            }
        }
    }
}
