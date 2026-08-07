package jobs.procrush.bootstrap.config

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.getcwd
import platform.posix.getenv as posixGetenv

@OptIn(ExperimentalForeignApi::class)
actual object PlatformEnv {
    actual fun getenv(name: String): String? = posixGetenv(name)?.toKString()

    actual fun workingDirectory(): String =
        memScoped {
            val buf = allocArray<ByteVar>(4096)
            getcwd(buf, 4096u)?.toKString() ?: "."
        }

    actual fun readLinesIfExists(absolutePath: String): List<String>? {
        val file = fopen(absolutePath, "r") ?: return null
        try {
            val lines = mutableListOf<String>()
            memScoped {
                val buf = allocArray<ByteVar>(8192)
                while (fgets(buf, 8192, file) != null) {
                    lines += buf.toKString().trimEnd('\n', '\r')
                }
            }
            return lines
        } finally {
            fclose(file)
        }
    }

    actual fun parentDirectory(path: String): String? {
        val normalized = path.trimEnd('/', '\\')
        val slash = normalized.lastIndexOf('/')
        val backslash = normalized.lastIndexOf('\\')
        val idx = maxOf(slash, backslash)
        return when {
            idx < 0 -> null
            idx == 0 -> normalized.take(1)
            else -> normalized.substring(0, idx)
        }
    }
}
