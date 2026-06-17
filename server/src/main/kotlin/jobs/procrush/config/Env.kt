package jobs.procrush.config

internal object Env {
    fun resolve(name: String, dotEnv: Map<String, String>): String? =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: dotEnv[name]?.takeIf { it.isNotBlank() }

    fun env(name: String, default: String, dotEnv: Map<String, String>): String =
        resolve(name, dotEnv) ?: default

    fun parseOrigins(raw: String): List<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
}
