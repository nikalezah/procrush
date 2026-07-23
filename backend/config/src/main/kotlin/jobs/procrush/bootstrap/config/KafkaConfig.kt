package jobs.procrush.bootstrap.config

data class KafkaConfig(
    val bootstrapServers: String,
    val matchingEventsTopic: String = "procrush.matching.events",
    val matchingEventsDlqTopic: String = "procrush.matching.events.dlq",
    val matchingResultsTopic: String = "procrush.matching.results",
    val consumerGroupId: String = "matching",
    val dedupTtlSeconds: Long = 3600,
) {
    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>): KafkaConfig = KafkaConfig(
            Env.resolve("KAFKA_BOOTSTRAP_SERVERS", dotEnv)
                ?: error("KAFKA_BOOTSTRAP_SERVERS is required (e.g. localhost:9092)"),
            Env.env("KAFKA_MATCHING_EVENTS_TOPIC", "procrush.matching.events", dotEnv),
            Env.env("KAFKA_MATCHING_EVENTS_DLQ_TOPIC", "procrush.matching.events.dlq", dotEnv),
            Env.env("KAFKA_MATCHING_RESULTS_TOPIC", "procrush.matching.results", dotEnv),
            Env.env("KAFKA_MATCHING_CONSUMER_GROUP", "matching", dotEnv),
            Env.env("KAFKA_DEDUP_TTL_SECONDS", "3600", dotEnv).toLong(),
        )
    }
}
