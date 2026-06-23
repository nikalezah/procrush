package jobs.procrush.matching.runtime.messaging

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.matching.events.MatchingEventEnvelope
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.matching.events.MatchingEventTypes
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
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
                                processRecord(record.value())
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
        logger.info("Matching event consumer started on topic {}", kafkaConfig.matchingEventsTopic)
    }

    fun stop() {
        running.set(false)
        thread?.join(5_000)
        runCatching { consumer?.close() }
        consumer = null
        thread = null
    }

    fun isRunning(): Boolean = running.get()

    private fun processRecord(body: String) {
        val envelope =
            runCatching {
                MatchingEventJson.json.decodeFromString(MatchingEventEnvelope.serializer(), body)
            }.getOrElse { error ->
                logger.error("Invalid matching event payload", error)
                return
            }

        if (!dedup.tryMarkProcessing(envelope.eventId)) {
            logger.info("Duplicate matching event eventId={}, skipping", envelope.eventId)
            return
        }

        try {
            when (envelope.eventType) {
                MatchingEventTypes.SEEKER_PROFILE_CHANGED -> {
                    val payload = MatchingEventJson.decodePayload<jobs.procrush.matching.events.SeekerProfileChangedPayload>(envelope)
                    processor.processSeekerProfileChanged(payload)
                }
                MatchingEventTypes.SEEKER_PERSONALITY_READY -> {
                    val payload = MatchingEventJson.decodePayload<jobs.procrush.matching.events.SeekerPersonalityReadyPayload>(envelope)
                    processor.processSeekerPersonalityReady(payload)
                }
                MatchingEventTypes.JOB_PROFILE_CHANGED -> {
                    val payload = MatchingEventJson.decodePayload<jobs.procrush.matching.events.JobProfileChangedPayload>(envelope)
                    processor.processJobProfileChanged(payload)
                }
                else -> logger.warn("Unknown matching event type: {}", envelope.eventType)
            }
        } catch (error: Exception) {
            logger.error("Failed to process matching event eventId={}", envelope.eventId, error)
            dlqProducer.send(
                ProducerRecord(
                    kafkaConfig.matchingEventsDlqTopic,
                    envelope.eventId,
                    body,
                ),
            ).get()
        } finally {
            dedup.release(envelope.eventId)
        }
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
