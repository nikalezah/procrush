package jobs.procrush.observability

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jobs.procrush.bootstrap.config.ObservabilityConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

object AppMetrics {
    lateinit var registry: MeterRegistry
        private set

    private var initialized = false
    private val matchingConsumerRunning = AtomicInteger(0)
    private val personalityConsumerRunning = AtomicInteger(0)
    private val queueDepths = ConcurrentHashMap<String, AtomicReference<Double>>()

    fun initialize(config: ObservabilityConfig): PrometheusMeterRegistry {
        if (initialized) {
            @Suppress("UNCHECKED_CAST")
            return registry as PrometheusMeterRegistry
        }
        val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        prometheusRegistry.config().commonTags(
            "service",
            config.serviceName,
            "environment",
            config.environment,
        )
        registry = prometheusRegistry
        Gauge.builder("matching.kafka.consumer.running", matchingConsumerRunning) { it.get().toDouble() }
            .register(prometheusRegistry)
        Gauge.builder("personality.rabbit.consumer.running", personalityConsumerRunning) { it.get().toDouble() }
            .register(prometheusRegistry)
        initialized = true
        return prometheusRegistry
    }

    fun redisCacheHit(cache: String) {
        if (!initialized) return
        registry.counter("redis.cache.hit", "cache", cache).increment()
    }

    fun redisCacheMiss(cache: String) {
        if (!initialized) return
        registry.counter("redis.cache.miss", "cache", cache).increment()
    }

    fun personalityJobProcessed(outcome: String) {
        if (!initialized) return
        registry.counter("personality.job.processed", "outcome", outcome).increment()
    }

    fun personalityJobDlq() {
        if (!initialized) return
        registry.counter("personality.job.dlq").increment()
    }

    fun matchingEventProcessed(eventType: String) {
        if (!initialized) return
        registry.counter("matching.event.processed", "event_type", eventType).increment()
    }

    fun matchingEventDlq() {
        if (!initialized) return
        registry.counter("matching.event.dlq").increment()
    }

    fun kafkaPublishFailure() {
        if (!initialized) return
        registry.counter("kafka.publish.failures").increment()
    }

    fun personalityLlmDuration(): Timer.Sample? =
        if (!initialized) {
            null
        } else {
            Timer.start(registry)
        }

    fun recordPersonalityLlmDurationFromNanos(startedNanos: Long) {
        if (!initialized) return
        val durationMs = java.time.Duration.ofNanos(System.nanoTime() - startedNanos).toMillis()
        registry.timer("personality.llm.duration").record(java.time.Duration.ofMillis(durationMs))
    }

    suspend fun <T> recordPersonalityLlm(block: suspend () -> T): T {
        val started = System.nanoTime()
        return try {
            block()
        } finally {
            recordPersonalityLlmDurationFromNanos(started)
        }
    }

    fun recordPersonalityLlmDuration(sample: Timer.Sample) {
        if (!initialized) return
        sample.stop(registry.timer("personality.llm.duration"))
    }

    fun matchingRecalculationDuration(): Timer.Sample? =
        if (!initialized) {
            null
        } else {
            Timer.start(registry)
        }

    fun <T> recordMatchingRecalculation(block: () -> T): T {
        val sample = matchingRecalculationDuration()
        return try {
            block()
        } finally {
            sample?.let { recordMatchingRecalculationDuration(it) }
        }
    }

    fun recordMatchingRecalculationDuration(sample: Timer.Sample) {
        if (!initialized) return
        sample.stop(registry.timer("matching.recalculation.duration"))
    }

    fun recordMatchingRecalculationDurationFromNanos(startedNanos: Long) {
        if (!initialized) return
        val durationMs = java.time.Duration.ofNanos(System.nanoTime() - startedNanos).toMillis()
        registry.timer("matching.recalculation.duration").record(java.time.Duration.ofMillis(durationMs))
    }

    fun setMatchingConsumerRunning(running: Boolean) {
        matchingConsumerRunning.set(if (running) 1 else 0)
    }

    fun setPersonalityConsumerRunning(running: Boolean) {
        personalityConsumerRunning.set(if (running) 1 else 0)
    }

    fun setRabbitMqQueueDepth(
        queue: String,
        depth: Double,
    ) {
        if (!initialized) return
        val holder =
            queueDepths.computeIfAbsent(queue) {
                val value = AtomicReference(depth)
                Gauge.builder("rabbitmq.queue.messages", value) { it.get() }
                    .tags(listOf(Tag.of("queue", queue)))
                    .register(registry)
                value
            }
        holder.set(depth)
    }
}
