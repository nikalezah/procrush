package jobs.procrush.personality

import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.bootstrap.redis.RedisDistributedLock
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.messaging.PersonalityCommandPublisher
import jobs.procrush.personality.messaging.PersonalityResultConsumer
import jobs.procrush.personality.messaging.PersonalityResultDedup
import jobs.procrush.personality.service.PersonalityGenerationCoordinator
import jobs.procrush.personality.service.PersonalityGenerationLockGuard
import jobs.procrush.personality.service.PersonalityProfileReader
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.personality.service.PersonalityResultApplyService
import jobs.procrush.personality.service.RedisPersonalityStatusNotifier
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.seeker.repository.SeekerSuperpowersAndTalentsRepository
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

data class PersonalityRuntime(
    val coordinator: PersonalityGenerationCoordinator,
    val personalityProfileService: PersonalityProfileService,
    private val personalityStatusNotifier: RedisPersonalityStatusNotifier,
    private val resultConsumer: PersonalityResultConsumer,
) {
    fun start() {
        personalityStatusNotifier.start()
        resultConsumer.start()
    }

    fun close() {
        resultConsumer.stop()
        personalityStatusNotifier.close()
    }

    companion object {
        fun create(
            redisConfig: RedisConfig,
            rabbitMqConfig: RabbitMqConfig,
            distributedLock: RedisDistributedLock,
            redis: RedisClient,
            messagePublisher: MessagePublisher,
            messageConsumer: MessageConsumer,
            seekerRepository: SeekerRepository,
            referenceRepository: ReferenceRepository,
            surveyService: SurveyService,
            matchingCache: MatchingCachePort,
            matchingEvents: MatchingEventPort,
            scope: CoroutineScope,
        ): PersonalityRuntime {
            val profileRepository = SeekerPersonalProfileRepository()
            val superpowersRepository = SeekerSuperpowersAndTalentsRepository()
            val lockGuard = PersonalityGenerationLockGuard(distributedLock, redisConfig)
            val commandLog = LoggerFactory.getLogger(PersonalityCommandPublisher::class.java)
            val publisher =
                PersonalityCommandPublisher(
                    messagePublisher,
                    rabbitMqConfig,
                    { message, args -> commandLog.info(message, *args) },
                )
            val personalityStatusNotifier =
                RedisPersonalityStatusNotifier(
                    redis = redis,
                    config = redisConfig,
                    scope = scope,
                )
            val coordinator =
                PersonalityGenerationCoordinator(
                    seekerRepository = seekerRepository,
                    profileRepository = profileRepository,
                    referenceRepository = referenceRepository,
                    surveyService = surveyService,
                    lockGuard = lockGuard,
                    publisher = publisher,
                    matchingCache = matchingCache,
                    matchingEvents = matchingEvents,
                )
            val reader =
                PersonalityProfileReader(
                    seekerRepository = seekerRepository,
                    profileRepository = profileRepository,
                    superpowersRepository = superpowersRepository,
                    surveyService = surveyService,
                    lockGuard = lockGuard,
                )
            val personalityProfileService =
                PersonalityProfileService(
                    reader = reader,
                    coordinator = coordinator,
                    surveyService = surveyService,
                    notifier = personalityStatusNotifier,
                )
            val applyService =
                PersonalityResultApplyService(
                    profileRepository = profileRepository,
                    referenceRepository = referenceRepository,
                    lockGuard = lockGuard,
                    statusNotifier = personalityStatusNotifier,
                    matchingCache = matchingCache,
                    matchingEvents = matchingEvents,
                )
            val resultConsumer =
                PersonalityResultConsumer(
                    messageConsumer = messageConsumer,
                    applyService = applyService,
                    dedup =
                        PersonalityResultDedup(
                            redis = redis,
                            config = redisConfig,
                            rabbitMqConfig = rabbitMqConfig,
                        ),
                    rabbitMqConfig = rabbitMqConfig,
                )
            return PersonalityRuntime(
                coordinator = coordinator,
                personalityProfileService = personalityProfileService,
                personalityStatusNotifier = personalityStatusNotifier,
                resultConsumer = resultConsumer,
            )
        }
    }
}
