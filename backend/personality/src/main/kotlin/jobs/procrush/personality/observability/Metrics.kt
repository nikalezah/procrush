package jobs.procrush.personality.observability

import kotlin.time.Duration
import kotlin.time.TimeSource

object Metrics {
    private val lock = Any()
    private var initialized = false
    private var serviceName: String = "personality"
    private var environment: String = "local"

    private var consumerRunning = 0
    private val jobProcessed = mutableMapOf<String, Long>()
    private var jobDlq = 0L
    private var llmDurationCount = 0L
    private var llmDurationSumNanos = 0L
    private var llmDurationMaxNanos = 0L
    private val queueDepths = mutableMapOf<String, Double>()

    fun initialize(config: WorkerLogConfig) {
        synchronized(lock) {
            if (initialized) return
            serviceName = config.serviceName
            environment = config.environment
            initialized = true
        }
    }

    fun setPersonalityConsumerRunning(running: Boolean) {
        synchronized(lock) {
            consumerRunning = if (running) 1 else 0
        }
    }

    fun personalityJobProcessed(outcome: String) {
        synchronized(lock) {
            if (!initialized) return
            jobProcessed[outcome] = (jobProcessed[outcome] ?: 0L) + 1L
        }
    }

    fun personalityJobDlq() {
        synchronized(lock) {
            if (!initialized) return
            jobDlq += 1L
        }
    }

    fun recordPersonalityLlmDuration(elapsed: Duration) {
        synchronized(lock) {
            if (!initialized) return
            val elapsedNanos = elapsed.inWholeNanoseconds.coerceAtLeast(0)
            llmDurationCount += 1L
            llmDurationSumNanos += elapsedNanos
            llmDurationMaxNanos = maxOf(llmDurationMaxNanos, elapsedNanos)
        }
    }

    suspend fun <T> recordPersonalityLlm(block: suspend () -> T): T {
        val started = TimeSource.Monotonic.markNow()
        return try {
            block()
        } finally {
            recordPersonalityLlmDuration(started.elapsedNow())
        }
    }

    fun setRabbitMqQueueDepth(
        queue: String,
        depth: Double,
    ) {
        synchronized(lock) {
            if (!initialized) return
            queueDepths[queue] = depth
        }
    }

    fun scrape(): String {
        synchronized(lock) {
            if (!initialized) return ""
            val common = labels("service" to serviceName, "environment" to environment)
            val builder = StringBuilder()

            builder.appendLine("# HELP personality_rabbit_consumer_running Whether the personality RabbitMQ consumer is running")
            builder.appendLine("# TYPE personality_rabbit_consumer_running gauge")
            builder.appendLine("personality_rabbit_consumer_running$common $consumerRunning")

            builder.appendLine("# HELP personality_job_processed_total Personality generation jobs processed")
            builder.appendLine("# TYPE personality_job_processed_total counter")
            if (jobProcessed.isEmpty()) {
                builder.appendLine(
                    "personality_job_processed_total${labels("outcome" to "success", "service" to serviceName, "environment" to environment)} 0",
                )
            } else {
                jobProcessed.entries.sortedBy { it.key }.forEach { (outcome, count) ->
                    builder.appendLine(
                        "personality_job_processed_total${labels("outcome" to outcome, "service" to serviceName, "environment" to environment)} $count",
                    )
                }
            }

            builder.appendLine("# HELP personality_job_dlq_total Personality generation jobs sent to DLQ")
            builder.appendLine("# TYPE personality_job_dlq_total counter")
            builder.appendLine("personality_job_dlq_total$common $jobDlq")

            val count = llmDurationCount
            val sumSeconds = llmDurationSumNanos.toDouble() / 1_000_000_000.0
            val maxSeconds = llmDurationMaxNanos.toDouble() / 1_000_000_000.0
            builder.appendLine("# HELP personality_llm_duration_seconds Personality LLM call duration")
            builder.appendLine("# TYPE personality_llm_duration_seconds summary")
            builder.appendLine("personality_llm_duration_seconds_count$common $count")
            builder.appendLine("personality_llm_duration_seconds_sum$common $sumSeconds")
            builder.appendLine("personality_llm_duration_seconds_max$common $maxSeconds")

            builder.appendLine("# HELP rabbitmq_queue_messages RabbitMQ queue depth")
            builder.appendLine("# TYPE rabbitmq_queue_messages gauge")
            queueDepths.entries.sortedBy { it.key }.forEach { (queue, depth) ->
                builder.appendLine(
                    "rabbitmq_queue_messages${labels("queue" to queue, "service" to serviceName, "environment" to environment)} $depth",
                )
            }

            return builder.toString()
        }
    }

    private fun labels(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            """$key="${escapeLabel(value)}""""
        }

    private fun escapeLabel(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
