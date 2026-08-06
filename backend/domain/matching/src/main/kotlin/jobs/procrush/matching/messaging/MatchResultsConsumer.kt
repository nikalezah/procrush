package jobs.procrush.matching.messaging

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.matching.events.MatchResultsUpdatedPayload
import jobs.procrush.matching.events.MatchingEventEnvelope
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.matching.events.MatchingEventTypes
import jobs.procrush.matching.service.MatchResultsApplyService
import jobs.procrush.shared.CorrelationIds
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.ObservabilityHolder
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

class MatchResultsConsumer(
    private val kafkaConfig: KafkaConfig,
    private val applyService: MatchResultsApplyService,
    private val dedup: MatchResultsEventDedup,
) {
    private val logger = LoggerFactory.getLogger(MatchResultsConsumer::class.java)
    private val running = AtomicBoolean(false)
    private var consumer: KafkaConsumer<String, String>? = null
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        val props =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.resultsConsumerGroupId)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "25")
            }
        val kafkaConsumer = KafkaConsumer<String, String>(props)
        kafkaConsumer.subscribe(listOf(kafkaConfig.matchingResultsTopic))
        consumer = kafkaConsumer
        thread =
            Thread(
                {
                    while (running.get()) {
                        try {
                            val records = kafkaConsumer.poll(Duration.ofMillis(500))
                            for (record in records) {
                                processRecord(record)
                            }
                            if (records.count() > 0) {
                                kafkaConsumer.commitSync()
                            }
                        } catch (error: Exception) {
                            logger.error("Match results consumer loop failed", error)
                        }
                    }
                },
                "api-match-results-consumer",
            ).also {
                it.isDaemon = true
                it.start()
            }
        logger.info(
            "Match results consumer started on topic {} group={}",
            kafkaConfig.matchingResultsTopic,
            kafkaConfig.resultsConsumerGroupId,
        )
    }

    fun stop() {
        running.set(false)
        thread?.join(5_000)
        runCatching { consumer?.close() }
        consumer = null
        thread = null
    }

    fun isRunning(): Boolean = running.get()

    private fun processRecord(record: ConsumerRecord<String, String>) {
        ObservabilityHolder.tracing.withKafkaRecord(record) {
            processRecordBody(record.value())
        }
    }

    private fun processRecordBody(body: String) {
        val envelope =
            runCatching {
                MatchingEventJson.json.decodeFromString(MatchingEventEnvelope.serializer(), body)
            }.getOrElse { error ->
                logger.error("Invalid match results event payload", error)
                return
            }

        MdcContext.runWith(
            mapOf(
                CorrelationIds.EVENT_ID to envelope.eventId,
                CorrelationIds.REQUEST_ID to envelope.correlationId,
            ),
        ) {
            if (envelope.eventType != MatchingEventTypes.MATCH_RESULTS_UPDATED) {
                logger.warn("Ignoring unexpected event type on results topic: {}", envelope.eventType)
                return@runWith
            }
            if (!dedup.tryMarkProcessing(envelope.eventId)) {
                logger.info("Duplicate match results event eventId={}, skipping", envelope.eventId)
                return@runWith
            }
            try {
                val payload = MatchingEventJson.decodePayload<MatchResultsUpdatedPayload>(envelope)
                applyService.apply(payload)
            } catch (error: Exception) {
                logger.error("Failed to apply match results eventId={}", envelope.eventId, error)
            } finally {
                dedup.release(envelope.eventId)
            }
        }
    }
}
