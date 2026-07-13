package jobs.procrush.gradle.i18n

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class NodeAvailableValueSource
@Inject constructor(
    private val execOperations: ExecOperations,
) : ValueSource<Boolean, NodeAvailableValueSource.Params> {

    interface Params : ValueSourceParameters

    override fun obtain(): Boolean {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val nodeCmd = if (isWindows) "node.exe" else "node"
        return try {
            val stderr = ByteArrayOutputStream()
            val result: ExecResult = execOperations.exec {
                commandLine(nodeCmd, "--version")
                isIgnoreExitValue = true
                standardOutput = stderr
                errorOutput = stderr
            }
            result.exitValue == 0
        } catch (_: Exception) {
            false
        }
    }
}
