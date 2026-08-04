package jobs.procrush.gradle.spektor

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Spektor on Windows emits a leading "_" in package segments. Copy into a clean
 * output tree with underscores stripped, so compileKotlin never fingerprints the
 * pre-rename paths.
 */
abstract class NormalizeSpektorPackagesTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        description = "Normalize Spektor package paths (strip leading underscores on Windows)"
    }

    @TaskAction
    fun normalize() {
        val source = sourceDir.get().asFile
        val output = outputDir.get().asFile
        if (output.exists()) {
            check(output.deleteRecursively()) { "Failed to delete ${output.path}" }
        }
        if (!source.exists()) {
            output.mkdirs()
            return
        }

        source.walkTopDown().forEach { file ->
            val relative = file.relativeTo(source)
            val normalizedRelative =
                relative.path
                    .split(File.separatorChar)
                    .joinToString(File.separator) { segment -> segment.removePrefix("_") }
            val target = File(output, normalizedRelative)
            if (file.isDirectory) {
                target.mkdirs()
                return@forEach
            }
            if (file.extension != "kt") {
                target.parentFile.mkdirs()
                file.copyTo(target, overwrite = true)
                return@forEach
            }
            target.parentFile.mkdirs()
            val text = file.readText()
            val fixed = text.replace("jobs.procrush.api.generated._", "jobs.procrush.api.generated.")
            target.writeText(fixed)
        }
    }
}
