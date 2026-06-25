package jobs.procrush.bootstrap.config

data class WorkerAppConfig(
    val workerHealthPort: Int,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val rabbitMq: RabbitMqConfig,
    val kafka: KafkaConfig,
    val llm: LlmConfig,
) {
    val databaseUrl: String get() = database.jdbcUrl
    val databaseUser: String get() = database.user
    val databasePassword: String get() = database.password

    companion object {
        fun fromEnvironment(): WorkerAppConfig {
            val dotEnv = DotEnv.load()
            val frontendUrl = Env.env("FRONTEND_URL", "http://localhost:8081", dotEnv)
            val database =
                DatabaseConfig.resolve(
                    databaseUrl = Env.resolve("DATABASE_URL", dotEnv),
                    databaseUser = Env.resolve("DATABASE_USER", dotEnv),
                    databasePassword = Env.resolve("DATABASE_PASSWORD", dotEnv),
                )
            return WorkerAppConfig(
                workerHealthPort =
                    Env.resolve("WORKER_HEALTH_PORT", dotEnv)?.toIntOrNull()
                        ?: Env.resolve("PORT", dotEnv)?.toIntOrNull()
                        ?: 8091,
                database = database,
                redis = RedisConfig.fromEnvironment(dotEnv),
                rabbitMq = RabbitMqConfig.fromEnvironment(dotEnv),
                kafka = KafkaConfig.fromEnvironment(dotEnv),
                llm = LlmConfig.fromEnvironment(dotEnv, frontendUrl),
            )
        }
    }
}

fun WorkerAppConfig.toAuthAppConfig(): AppConfig =
    AppConfig(
        port = 0,
        database = database,
        redis = redis,
        rabbitMq = rabbitMq,
        kafka = kafka,
        matchingServiceUrl = "http://localhost:8092",
        webOrigins = listOf("http://localhost:8081"),
        sessionCookieName = "procrush_session",
        sessionDays = 30,
        authDevMode = true,
        cookieSecure = false,
    )
