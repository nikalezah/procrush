package jobs.procrush.personality.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.LongAdder

object Metrics {
    private var initialized = false
    private var serviceName: String = "personality"
    private var environment: String = "local"

    private val consumerRunning = AtomicInteger(0)
    private val jobProcessed = ConcurrentHashMap<String, AtomicLong>()
    private val jobDlq = AtomicLong(0)
    private val llmDurationCount = AtomicLong(0)
    private val llmDurationSumNanos = LongAdder()
    private val llmDurationMaxNanos = AtomicLong(0)
    private val queueDepths = ConcurrentHashMap<String, AtomicReference<Double>>()

    @Synchronized
    fun initialize(config: WorkerLogConfig) {
        if (initialized) return
        serviceName = config.serviceName
        environment = config.environment
        initialized = true
    }

    fun setPersonalityConsumerRunning(running: Boolean) {
        consumerRunning.set(if (running) 1 else 0)
    }

    fun personalityJobProcessed(outcome: String) {
        if (!initialized) return
        jobProcessed.computeIfAbsent(outcome) { AtomicLong(0) }.incrementAndGet()
    }

    fun personalityJobDlq() {
        if (!initialized) return
        jobDlq.incrementAndGet()
    }

    fun recordPersonalityLlmDurationFromNanos(startedNanos: Long) {
        if (!initialized) return
        val elapsed = (System.nanoTime() - startedNanos).coerceAtLeast(0)
        llmDurationCount.incrementAndGet()
        llmDurationSumNanos.add(elapsed)
        llmDurationMaxNanos.updateAndGet { current -> maxOf(current, elapsed) }
    }

    suspend fun <T> recordPersonalityLlm(block: suspend () -> T): T {
        val started = System.nanoTime()
        return try {
            block()
        } finally {
            recordPersonalityLlmDurationFromNanos(started)
        }
    }

    fun setRabbitMqQueueDepth(
        queue: String,
        depth: Double,
    ) {
        if (!initialized) return
        queueDepths.computeIfAbsent(queue) { AtomicReference(0.0) }.set(depth)
    }

    fun scrape(): String {
        if (!initialized) return ""
        val common = labels("service" to serviceName, "environment" to environment)
        val builder = StringBuilder()

        builder.appendLine("# HELP personality_rabbit_consumer_running Whether the personality RabbitMQ consumer is running")
        builder.appendLine("# TYPE personality_rabbit_consumer_running gauge")
        builder.appendLine("personality_rabbit_consumer_running$common ${consumerRunning.get()}")

        builder.appendLine("# HELP personality_job_processed_total Personality generation jobs processed")
        builder.appendLine("# TYPE personality_job_processed_total counter")
        if (jobProcessed.isEmpty()) {
            builder.appendLine("personality_job_processed_total${labels("outcome" to "success", "service" to serviceName, "environment" to environment)} 0")
        } else {
            jobProcessed.entries.sortedBy { it.key }.forEach { (outcome, count) ->
                builder.appendLine(
                    "personality_job_processed_total${labels("outcome" to outcome, "service" to serviceName, "environment" to environment)} ${count.get()}",
                )
            }
        }

        builder.appendLine("# HELP personality_job_dlq_total Personality generation jobs sent to DLQ")
        builder.appendLine("# TYPE personality_job_dlq_total counter")
        builder.appendLine("personality_job_dlq_total$common ${jobDlq.get()}")

        val count = llmDurationCount.get()
        val sumSeconds = llmDurationSumNanos.sum().toDouble() / 1_000_000_000.0
        val maxSeconds = llmDurationMaxNanos.get().toDouble() / 1_000_000_000.0
        builder.appendLine("# HELP personality_llm_duration_seconds Personality LLM call duration")
        builder.appendLine("# TYPE personality_llm_duration_seconds summary")
        builder.appendLine("personality_llm_duration_seconds_count$common $count")
        builder.appendLine("personality_llm_duration_seconds_sum$common $sumSeconds")
        builder.appendLine("personality_llm_duration_seconds_max$common $maxSeconds")

        builder.appendLine("# HELP rabbitmq_queue_messages RabbitMQ queue depth")
        builder.appendLine("# TYPE rabbitmq_queue_messages gauge")
        queueDepths.entries.sortedBy { it.key }.forEach { (queue, depth) ->
            builder.appendLine(
                "rabbitmq_queue_messages${labels("queue" to queue, "service" to serviceName, "environment" to environment)} ${depth.get()}",
            )
        }

        return builder.toString()
    }

    private fun labels(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            """$key="${escapeLabel(value)}""""
        }

    private fun escapeLabel(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
