package jobs.procrush.bootstrap.config

data class RedisConfig(
    val url: String,
    val keyPrefix: String = "procrush:",
    val commandTimeoutSeconds: Long = 5,
    val recommendationCacheTtlSeconds: Long = 600,
    val personalityLockTtlSeconds: Long = 300,
) {
    fun key(vararg parts: String): String = keyPrefix + parts.joinToString(":")

    fun matchEventsChannel(userId: String): String = key("events", "match")

    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>): RedisConfig {
            val url =
                Env.resolve("REDIS_URL", dotEnv)
                    ?: error("REDIS_URL is required (e.g. redis://localhost:6379)")
            return RedisConfig(
                url = url,
                keyPrefix = Env.env("REDIS_KEY_PREFIX", "procrush:", dotEnv),
                commandTimeoutSeconds = Env.env("REDIS_COMMAND_TIMEOUT_SECONDS", "5", dotEnv).toLong(),
                recommendationCacheTtlSeconds =
                    Env.env("REDIS_RECOMMENDATION_CACHE_TTL_SECONDS", "600", dotEnv).toLong(),
                personalityLockTtlSeconds =
                    Env.env("REDIS_PERSONALITY_LOCK_TTL_SECONDS", "300", dotEnv).toLong(),
            )
        }
    }
}
