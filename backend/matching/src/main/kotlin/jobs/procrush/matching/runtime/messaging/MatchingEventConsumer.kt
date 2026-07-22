package jobs.procrush.matching.runtime.messaging

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.MatchingEventEnvelope
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.matching.events.MatchingEventTypes
import jobs.procrush.matching.events.SeekerPersonalityReadyPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import jobs.procrush.observability.AppMetrics
import jobs.procrush.observability.CorrelationIds
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.ObservabilityHolder
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

class MatchingEventConsumer(
    private val kafkaConfig: KafkaConfig,
    private val processor: MatchingEventProcessor,
    private val dedup: MatchingEventDedup,
    private val dlqProducer: KafkaProducer<String, String>,
) {
    private val logger = LoggerFactory.getLogger(MatchingEventConsumer::class.java)
    private val running = AtomicBoolean(false)
    private var consumer: KafkaConsumer<String, String>? = null
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        val props =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.consumerGroupId)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10")
            }
        val kafkaConsumer = KafkaConsumer<String, String>(props)
        kafkaConsumer.subscribe(listOf(kafkaConfig.matchingEventsTopic))
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
                            logger.error("Matching event consumer loop failed", error)
                        }
                    }
                },
                "matching-event-consumer",
            ).also { it.isDaemon = true; it.start() }
        AppMetrics.setMatchingConsumerRunning(true)
        logger.info("Matching event consumer started on topic {}", kafkaConfig.matchingEventsTopic)
    }

    fun stop() {
        running.set(false)
        thread?.join(5_000)
        runCatching { consumer?.close() }
        consumer = null
        thread = null
        AppMetrics.setMatchingConsumerRunning(false)
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
                logger.error("Invalid matching event payload", error)
                sendToDlq(body, "invalid_json")
                return
            }

        MdcContext.runWith(
            mapOf(
                CorrelationIds.EVENT_ID to envelope.eventId,
                CorrelationIds.REQUEST_ID to envelope.correlationId,
            ),
        ) {
            processEnvelope(envelope, body)
        }
    }

    private fun processEnvelope(
        envelope: MatchingEventEnvelope,
        body: String,
    ) {
        if (!dedup.tryMarkProcessing(envelope.eventId)) {
            logger.info("Duplicate matching event eventId={}, skipping", envelope.eventId)
            return
        }

        val durationStarted = System.nanoTime()
        try {
            when (envelope.eventType) {
                MatchingEventTypes.SEEKER_PROFILE_CHANGED -> {
                    val payload = MatchingEventJson.decodePayload<SeekerProfileChangedPayload>(envelope)
                    MdcContext.put(CorrelationIds.SEEKER_ID, payload.seekerId.toString())
                    processor.processSeekerProfileChanged(payload)
                    AppMetrics.matchingEventProcessed(envelope.eventType)
                }
                MatchingEventTypes.SEEKER_PERSONALITY_READY -> {
                    val payload = MatchingEventJson.decodePayload<SeekerPersonalityReadyPayload>(envelope)
                    MdcContext.put(CorrelationIds.SEEKER_ID, payload.seekerId.toString())
                    processor.processSeekerPersonalityReady(payload)
                    AppMetrics.matchingEventProcessed(envelope.eventType)
                }
                MatchingEventTypes.JOB_PROFILE_CHANGED -> {
                    val payload = MatchingEventJson.decodePayload<JobProfileChangedPayload>(envelope)
                    processor.processJobProfileChanged(payload)
                    AppMetrics.matchingEventProcessed(envelope.eventType)
                }
                else -> {
                    logger.warn("Unknown matching event type: {}", envelope.eventType)
                    sendToDlq(body, "unknown_event_type", envelope.eventId)
                }
            }
        } catch (error: Exception) {
            logger.error("Failed to process matching event eventId={}", envelope.eventId, error)
            sendToDlq(body, "failed_to_process", envelope.eventId)
        } finally {
            AppMetrics.recordMatchingRecalculationDurationFromNanos(durationStarted)
            dedup.release(envelope.eventId)
        }
    }

    private fun sendToDlq(
        body: String,
        reason: String,
        key: String? = null,
    ) {
        AppMetrics.matchingEventDlq()
        val record = ProducerRecord(kafkaConfig.matchingEventsDlqTopic, key, body)
        record.headers().add(RecordHeader("error.reason", reason.toByteArray(StandardCharsets.UTF_8)))
        record.headers().add(
            RecordHeader(
                "original.topic",
                kafkaConfig.matchingEventsTopic.toByteArray(StandardCharsets.UTF_8),
            ),
        )
        dlqProducer.send(record).get()
    }

    companion object {
        fun createDlqProducer(kafkaConfig: KafkaConfig): KafkaProducer<String, String> {
            val props =
                Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
                    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.ACKS_CONFIG, "all")
                }
            return KafkaProducer(props)
        }
    }
}
