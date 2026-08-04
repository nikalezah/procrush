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
        }
    }
}
