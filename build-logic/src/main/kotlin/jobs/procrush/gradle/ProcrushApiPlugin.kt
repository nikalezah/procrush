package jobs.procrush.gradle

import jobs.procrush.gradle.spektor.NormalizeSpektorPackagesTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProcrushApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val generatedDir = target.layout.buildDirectory.dir("spektor-generated/kotlin")

        val normalize = target.tasks.register(
            "normalizeSpektorPackages",
            NormalizeSpektorPackagesTask::class.java,
        ) {
            this.generatedDir.set(generatedDir)
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
        }
    }
}
