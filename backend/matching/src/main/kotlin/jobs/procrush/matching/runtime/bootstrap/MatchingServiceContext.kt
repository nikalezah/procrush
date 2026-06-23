package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.matching.runtime.messaging.MatchingEventConsumer
import jobs.procrush.matching.runtime.messaging.MatchingEventDedup
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import jobs.procrush.shared.repository.ReferenceRepository

class MatchingServiceContext private constructor(
    val config: MatchingServiceAppConfig,
    val redisModule: RedisModule,
    private val kafkaModule: KafkaModule,
    val eventConsumer: MatchingEventConsumer,
    val matchResultsRepository: MatchResultsRepository,
    private val dlqProducer: org.apache.kafka.clients.producer.KafkaProducer<String, String>,
) {
    fun close() {
        eventConsumer.stop()
        dlqProducer.close()
        kafkaModule.close()
        redisModule.close()
    }

    companion object {
        fun create(): MatchingServiceContext {
            val config = MatchingServiceAppConfig.fromEnvironment()
            MatchingDatabaseRegistry.init(
                mainConfig = config.mainDatabase,
                matchingConfig = config.matchingDatabase,
            )
            val redis = RedisModule.create(config.appConfig)
            val kafka = KafkaModule.create(config.kafka)
            val mainDb = MatchingDatabaseRegistry.main
            val referenceRepository = ReferenceRepository(mainDb)
            val matchingRepository = MatchingRepository(referenceRepository, mainDb)
            val matchResultsRepository = MatchResultsRepository()
            val processor = MatchingEventProcessor(matchingRepository, matchResultsRepository)
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
                dlqProducer = dlqProducer,
            )
        }
    }
}
