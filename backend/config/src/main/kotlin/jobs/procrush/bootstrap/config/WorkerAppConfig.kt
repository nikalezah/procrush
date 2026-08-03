package jobs.procrush.bootstrap.config

data class WorkerAppConfig(
    val workerHealthPort: Int,
    val rabbitMq: RabbitMqConfig,
    val llm: LlmConfig,
) {
    companion object {
        fun fromEnvironment(): WorkerAppConfig {
            val dotEnv = DotEnv.load()
            val frontendUrl = Env.env("FRONTEND_URL", "http://localhost:8081", dotEnv)
            return WorkerAppConfig(
                workerHealthPort =
                    Env.resolve("WORKER_HEALTH_PORT", dotEnv)?.toIntOrNull()
                        ?: Env.resolve("PORT", dotEnv)?.toIntOrNull()
                        ?: 8091,
                rabbitMq = RabbitMqConfig.fromEnvironment(dotEnv),
                llm = LlmConfig.fromEnvironment(dotEnv, frontendUrl),
            )
        }
    }
}
