package jobs.procrush.bootstrap.config

data class RabbitMqConfig(
    val url: String,
    val exchange: String = "procrush.personality",
    val deadLetterExchange: String = "procrush.personality.dlx",
    val queue: String = "personality.generation",
    val deadLetterQueue: String = "personality.generation.dlq",
    val routingKey: String = "generate",
    val deadLetterRoutingKey: String = "dlq",
    val prefetch: Int = 1,
    val maxRetries: Int = 3,
    val dedupTtlSeconds: Long = 3600,
) {
    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>): RabbitMqConfig {
            val url =
                Env.resolve("RABBITMQ_URL", dotEnv)
                    ?: error("RABBITMQ_URL is required (e.g. amqp://procrush:procrush@localhost:5672/%2F)")
            return RabbitMqConfig(
                url = url,
                queue = Env.env("RABBITMQ_PERSONALITY_QUEUE", "personality.generation", dotEnv),
                prefetch = Env.env("RABBITMQ_PREFETCH", "1", dotEnv).toInt(),
                maxRetries = Env.env("RABBITMQ_MAX_RETRIES", "3", dotEnv).toInt(),
            )
        }
    }
}
