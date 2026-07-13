package jobs.procrush.gradle.spektor

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Spektor on Windows emits a leading "_" in package segments; normalize for
 * cross-platform builds (Docker/kind).
 */
abstract class NormalizeSpektorPackagesTask : DefaultTask() {

    @get:Internal
    abstract val generatedDir: DirectoryProperty

    init {
        description = "Normalize Spektor package paths (strip leading underscores on Windows)"
    }

    @TaskAction
    fun normalize() {
        val root = generatedDir.get().asFile
        if (!root.exists()) {
            return
        }
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            val fixed = text.replace("jobs.procrush.api.generated._", "jobs.procrush.api.generated.")
            if (text != fixed) {
                file.writeText(fixed)
            }
        }
        root.walkTopDown()
            .filter { it.isDirectory && it.name.startsWith("_") }
            .toList()
            .sortedByDescending { it.path.length }
            .forEach { dir ->
                val target = File(dir.parentFile, dir.name.removePrefix("_"))
                if (target.exists()) {
                    check(target.deleteRecursively()) { "Failed to delete ${target.path}" }
                }
                check(dir.renameTo(target)) { "Failed to rename ${dir.path} -> ${target.path}" }
            }
    }
}
