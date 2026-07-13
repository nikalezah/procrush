package jobs.procrush.gradle.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class GenerateI18nTask
@Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @get:Input
    abstract val nodeCommand: Property<String>

    @get:Internal
    abstract val nodeAvailable: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workingDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val errorCodesYaml: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localesDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generateScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val validateScript: RegularFileProperty

    @get:OutputFile
    abstract val generatedKotlin: RegularFileProperty

    @get:OutputFile
    abstract val generatedTypescript: RegularFileProperty

    init {
        group = "i18n"
        description = "Generate ErrorCode from i18n/error-codes.yaml (requires Node.js)"
        onlyIf("Node.js available, or committed generated sources present") {
            if (nodeAvailable.get()) {
                true
            } else if (generatedKotlin.get().asFile.exists() && generatedTypescript.get().asFile.exists()) {
                logger.lifecycle(
                    "Skipping generateI18n: Node.js not available; using committed generated sources",
                )
                false
            } else {
                throw GradleException(
                    "generateI18n requires Node.js, and i18n/generated sources are missing",
                )
            }
        }
    }

    @TaskAction
    fun generate() {
        execOperations.exec {
            workingDir(this@GenerateI18nTask.workingDir.get().asFile)
            commandLine(nodeCommand.get(), "scripts/generate.mjs")
        }
    }
}
