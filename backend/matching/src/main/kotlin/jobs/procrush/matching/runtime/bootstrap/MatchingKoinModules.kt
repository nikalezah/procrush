package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.bootstrap.kafka.KafkaStringPublisher
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.runtime.messaging.MatchingEventConsumer
import jobs.procrush.matching.runtime.messaging.MatchingEventDedup
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.repository.MatchingProjectionRepository
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import org.apache.kafka.clients.producer.KafkaProducer
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun matchingKoinModules(config: MatchingServiceAppConfig): List<Module> =
    listOf(
        module {
            single { config }
            single { RedisModule.create(get<MatchingServiceAppConfig>().appConfig) }
            single<RedisClient> { get<RedisModule>().client }
            single { KafkaModule.create(get<MatchingServiceAppConfig>().kafka) }
            single { KafkaStringPublisher(get<KafkaModule>().producer, get<MatchingServiceAppConfig>().kafka.matchingResultsTopic) }
            singleOf(::MatchingProjectionRepository)
            singleOf(::MatchResultsRepository)
            singleOf(::MatchingEventProcessor)
            single { MatchingEventDedup(get(), get<MatchingServiceAppConfig>().redis, get<MatchingServiceAppConfig>().kafka) }
            single<KafkaProducer<String, String>> {
                MatchingEventConsumer.createDlqProducer(get<MatchingServiceAppConfig>().kafka)
            }
            single { MatchingEventConsumer(get<MatchingServiceAppConfig>().kafka, get(), get(), get()) }
            singleOf(::MatchingRuntime)
        },
    )

internal class MatchingRuntime(
    private val redisModule: RedisModule,
    private val kafkaModule: KafkaModule,
    private val eventConsumer: MatchingEventConsumer,
    private val dlqProducer: KafkaProducer<String, String>,
    private val resultsPublisher: KafkaStringPublisher,
) {
    fun start() {
        eventConsumer.start()
    }

    fun close() {
        eventConsumer.stop()
        resultsPublisher.flush()
        dlqProducer.close()
        kafkaModule.close()
        redisModule.close()
    }
}