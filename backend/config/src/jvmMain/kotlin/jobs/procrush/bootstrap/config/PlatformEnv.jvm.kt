package jobs.procrush.bootstrap.config

import java.nio.file.Files
import java.nio.file.Path

actual object PlatformEnv {
    actual fun getenv(name: String): String? = System.getenv(name)

    actual fun workingDirectory(): String = System.getProperty("user.dir")

    actual fun readLinesIfExists(absolutePath: String): List<String>? {
        val path = Path.of(absolutePath)
        if (!Files.exists(path)) return null
        return Files.readAllLines(path)
    }

    actual fun parentDirectory(path: String): String? = Path.of(path).parent?.toString()
}
