package jobs.procrush.bootstrap.config

import java.nio.file.Files
import java.nio.file.Path

/**
 * JVM-backed environment/file helpers. Call sites use only this API so later
 * expect/actual porting does not churn Env/DotEnv consumers.
 */
object PlatformEnv {
    fun getenv(name: String): String? = System.getenv(name)

    fun workingDirectory(): String = System.getProperty("user.dir")

    fun readLinesIfExists(absolutePath: String): List<String>? {
        val path = Path.of(absolutePath)
        if (!Files.exists(path)) return null
        return Files.readAllLines(path)
    }

    fun parentDirectory(path: String): String? = Path.of(path).parent?.toString()
}
