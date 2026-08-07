package jobs.procrush.gradle.personality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Links `personality.kexe` inside a Linux JDK container.
 *
 * Required on Windows hosts: Kotlin/Native's mingw `ld.gold` cannot resolve symbols from
 * ktor-client-curl's static OpenSSL (`libcrypto.a`) when cross-linking linuxX64.
 */
abstract class LinkPersonalityDockerTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputSources: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputKexe: RegularFileProperty

    init {
        group = "build"
        description =
            "Link personality.kexe in Linux Docker (Windows workaround for ktor-client-curl TLS)"
    }

    @TaskAction
    fun link() {
        val root = projectRoot.get().asFile
        val mount = root.absolutePath.replace('\\', '/')
        val result =
            execOperations.exec {
                workingDir = root
                // Separate Gradle home/project-cache from the host mount so Windows
                // file locks on .gradle/ do not break the nested Linux build.
                commandLine(
                    "docker",
                    "run",
                    "--rm",
                    "-v",
                    "$mount:/project",
                    "-v",
                    "procrush-personality-gradle-home:/gradle-home",
                    "-e",
                    "GRADLE_USER_HOME=/gradle-home",
                    "-w",
                    "/project",
                    "eclipse-temurin:25-jdk",
                    "./gradlew",
                    ":backend:personality:linkReleaseExecutableLinuxX64",
                    "--no-daemon",
                    "--project-cache-dir=/tmp/gradle-project-cache",
                )
                isIgnoreExitValue = true
            }
        if (result.exitValue != 0) {
            throw GradleException(
                "Docker link of personality.kexe failed (exit ${result.exitValue}). " +
                    "Ensure Docker Desktop is running.",
            )
        }
        val kexe = outputKexe.get().asFile
        if (!kexe.isFile) {
            throw GradleException("Expected personality binary missing: ${kexe.path}")
        }
    }
}
