package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.runtime.messaging.MatchingEventConsumer
import jobs.procrush.matching.runtime.messaging.MatchingEventDedup
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.repository.MatchingProjectionRepository
import jobs.procrush.matching.runtime.service.MatchResultsEventPublisher
import jobs.procrush.matching.runtime.service.MatchingEventProcessor

class MatchingServiceContext private constructor(
    val config: MatchingServiceAppConfig,
    val redisModule: RedisModule,
    private val kafkaModule: KafkaModule,
    val eventConsumer: MatchingEventConsumer,
    val matchResultsRepository: MatchResultsRepository,
    val projectionRepository: MatchingProjectionRepository,
    private val dlqProducer: org.apache.kafka.clients.producer.KafkaProducer<String, String>,
    private val resultsPublisher: MatchResultsEventPublisher,
) {
    fun close() {
        eventConsumer.stop()
        resultsPublisher.flush()
        dlqProducer.close()
        kafkaModule.close()
        redisModule.close()
    }

    companion object {
        fun create(): MatchingServiceContext {
            val config = MatchingServiceAppConfig.fromEnvironment()
            MatchingDatabaseRegistry.init(matchingConfig = config.matchingDatabase)
            val redis = RedisModule.create(config.appConfig)
            val kafka = KafkaModule.create(config.kafka)
            val projectionRepository = MatchingProjectionRepository()
            val matchResultsRepository = MatchResultsRepository()
            val resultsPublisher = MatchResultsEventPublisher(kafka.producer, config.kafka)
            val processor = MatchingEventProcessor(projectionRepository, matchResultsRepository, resultsPublisher)
            val dedup = MatchingEventDedup(redis.client, config.redis, config.kafka)
            val dlqProducer = MatchingEventConsumer.createDlqProducer(config.kafka)
            val eventConsumer =
                MatchingEventConsumer(
                    kafkaConfig = config.kafka,
                    processor = processor,
                    dedup = dedup,
                    dlqProducer = dlqProducer,
                )
            eventConsumer.start()
            return MatchingServiceContext(
                config = config,
                redisModule = redis,
                kafkaModule = kafka,
                eventConsumer = eventConsumer,
                matchResultsRepository = matchResultsRepository,
                projectionRepository = projectionRepository,
                dlqProducer = dlqProducer,
                resultsPublisher = resultsPublisher,
            )
        }
    }
}
