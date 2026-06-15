package jobs.procrush.config

data class AppConfig(
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val webOrigin: String,
    val webOrigins: List<String>,
    val frontendUrl: String,
    val sessionCookieName: String,
    val sessionDays: Long,
    val authDevMode: Boolean,
    val cookieSecure: Boolean,
    val llm: LlmConfig,
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val dotEnv = DotEnv.load()
            val webOrigin = env("WEB_ORIGIN", "http://localhost:8081,http://localhost:8082", dotEnv)
            val webOrigins = parseOrigins(webOrigin)
            val frontendUrl = env("FRONTEND_URL", "http://localhost:8081", dotEnv)
            val database =
                DatabaseConfig.resolve(
                    databaseUrl = resolve("DATABASE_URL", dotEnv),
                    databaseUser = resolve("DATABASE_USER", dotEnv),
                    databasePassword = resolve("DATABASE_PASSWORD", dotEnv),
                )
            return AppConfig(
                databaseUrl = database.jdbcUrl,
                databaseUser = database.user,
                databasePassword = database.password,
                webOrigin = webOrigin,
                webOrigins = webOrigins,
                frontendUrl = frontendUrl,
                sessionCookieName = env("SESSION_COOKIE_NAME", "procrush_session", dotEnv),
                sessionDays = env("SESSION_DAYS", "30", dotEnv).toLong(),
                authDevMode = authDevModeEnabled(dotEnv),
                cookieSecure = cookieSecureEnabled(dotEnv, webOrigins, frontendUrl),
                llm = LlmConfig.fromEnvironment(dotEnv, frontendUrl),
            )
        }

        private fun cookieSecureEnabled(
            dotEnv: Map<String, String>,
            webOrigins: List<String>,
            frontendUrl: String,
        ): Boolean {
            resolve("COOKIE_SECURE", dotEnv)?.let { raw ->
                return raw.equals("true", ignoreCase = true)
            }
            return (listOf(frontendUrl) + webOrigins).any { it.startsWith("https://", ignoreCase = true) }
        }

        private fun authDevModeEnabled(dotEnv: Map<String, String>): Boolean {
            val raw =
                resolve("AUTH_DEV_MODE", dotEnv)
                    ?: resolve("OAUTH_DEV_MOCK", dotEnv)
                    ?: "true"
            return raw.equals("true", ignoreCase = true)
        }

        private fun env(name: String, default: String, dotEnv: Map<String, String>): String =
            resolve(name, dotEnv) ?: default

        private fun resolve(name: String, dotEnv: Map<String, String>): String? =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: dotEnv[name]?.takeIf { it.isNotBlank() }

        private fun parseOrigins(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
    }
}
