package jobs.procrush.bootstrap.config

object DotEnv {
    fun load(): Map<String, String> {
        var dir = PlatformEnv.workingDirectory()
        while (true) {
            val envFile = joinPath(dir, ".env")
            val lines = PlatformEnv.readLinesIfExists(envFile)
            if (lines != null) {
                return lines.mapNotNull { parseLine(it) }.toMap()
            }
            dir = PlatformEnv.parentDirectory(dir) ?: break
        }
        return emptyMap()
    }

    private fun joinPath(dir: String, name: String): String =
        when {
            dir.endsWith("/") || dir.endsWith("\\") -> dir + name
            else -> "$dir/$name"
        }

    private fun parseLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        val separator = trimmed.indexOf('=')
        if (separator <= 0) return null
        val key = trimmed.substring(0, separator).trim()
        if (key.isEmpty()) return null
        var value = trimmed.substring(separator + 1).trim()
        if (
            value.length >= 2 &&
            ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'")))
        ) {
            value = value.substring(1, value.length - 1)
        }
        return key to value
    }
}
