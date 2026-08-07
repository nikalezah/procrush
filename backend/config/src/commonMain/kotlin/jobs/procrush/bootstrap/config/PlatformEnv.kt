package jobs.procrush.bootstrap.config

/**
 * Platform-backed environment/file helpers. Call sites use only this API so
 * JVM and Native consumers share Env/DotEnv without platform imports.
 */
expect object PlatformEnv {
    fun getenv(name: String): String?

    fun workingDirectory(): String

    fun readLinesIfExists(absolutePath: String): List<String>?

    fun parentDirectory(path: String): String?
}
