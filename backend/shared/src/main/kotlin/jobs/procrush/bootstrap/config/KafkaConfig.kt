package jobs.procrush.bootstrap.config

data class KafkaConfig(
    val bootstrapServers: String,
    val matchingEventsTopic: String = "procrush.matching.events",
    val matchingEventsDlqTopic: String = "procrush.matching.events.dlq",
    val consumerGroupId: String = "matching-service",
    val dedupTtlSeconds: Long = 3600,
) {
    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>): KafkaConfig {
            val bootstrapServers =
                Env.resolve("KAFKA_BOOTSTRAP_SERVERS", dotEnv)
                    ?: error("KAFKA_BOOTSTRAP_SERVERS is required (e.g. localhost:9092)")
            return KafkaConfig(
                bootstrapServers = bootstrapServers,
                matchingEventsTopic =
                    Env.env("KAFKA_MATCHING_EVENTS_TOPIC", "procrush.matching.events", dotEnv),
                matchingEventsDlqTopic =
                    Env.env("KAFKA_MATCHING_EVENTS_DLQ_TOPIC", "procrush.matching.events.dlq", dotEnv),
                consumerGroupId = Env.env("KAFKA_MATCHING_CONSUMER_GROUP", "matching-service", dotEnv),
                dedupTtlSeconds = Env.env("KAFKA_DEDUP_TTL_SECONDS", "3600", dotEnv).toLong(),
            )
        }
    }
}
