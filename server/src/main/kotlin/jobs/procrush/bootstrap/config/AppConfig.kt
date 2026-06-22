package jobs.procrush.bootstrap.config

data class AppConfig(
    val port: Int,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val webOrigins: List<String>,
    val sessionCookieName: String,
    val sessionDays: Long,
    val authDevMode: Boolean,
    val cookieSecure: Boolean,
    val llm: LlmConfig,
) {
    val databaseUrl: String get() = database.jdbcUrl
    val databaseUser: String get() = database.user
    val databasePassword: String get() = database.password

    companion object {
        fun fromEnvironment(): AppConfig {
            val dotEnv = DotEnv.load()
            val webOrigins = Env.parseOrigins(Env.env("WEB_ORIGIN", "http://localhost:8081,http://localhost:8082", dotEnv))
            val frontendUrl = Env.env("FRONTEND_URL", "http://localhost:8081", dotEnv)
            val database =
                DatabaseConfig.resolve(
                    databaseUrl = Env.resolve("DATABASE_URL", dotEnv),
                    databaseUser = Env.resolve("DATABASE_USER", dotEnv),
                    databasePassword = Env.resolve("DATABASE_PASSWORD", dotEnv),
                )
            return AppConfig(
                port = Env.env("PORT", "8080", dotEnv).toIntOrNull() ?: 8080,
                database = database,
                redis = RedisConfig.fromEnvironment(dotEnv),
                webOrigins = webOrigins,
                sessionCookieName = Env.env("SESSION_COOKIE_NAME", "procrush_session", dotEnv),
                sessionDays = Env.env("SESSION_DAYS", "30", dotEnv).toLong(),
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
            Env.resolve("COOKIE_SECURE", dotEnv)?.let { raw ->
                return raw.equals("true", ignoreCase = true)
            }
            return (listOf(frontendUrl) + webOrigins).any { it.startsWith("https://", ignoreCase = true) }
        }

        private fun authDevModeEnabled(dotEnv: Map<String, String>): Boolean {
            val raw =
                Env.resolve("AUTH_DEV_MODE", dotEnv)
                    ?: Env.resolve("OAUTH_DEV_MOCK", dotEnv)
                    ?: "true"
            return raw.equals("true", ignoreCase = true)
        }
    }
}
