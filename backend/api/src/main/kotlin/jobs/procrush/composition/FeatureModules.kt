package jobs.procrush.composition

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.cache.CachedMatchingService
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.messaging.MatchResultsConsumer
import jobs.procrush.matching.messaging.MatchResultsEventDedup
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.repository.MatchInterestRepository
import jobs.procrush.matching.repository.MatchScoreRepository
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.matching.service.LocalMatchingQueries
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.matching.service.MatchResultsApplyService
import jobs.procrush.matching.service.RecommendationsEventService
import jobs.procrush.matching.service.RedisMatchInterestNotifier
import jobs.procrush.matching.service.RedisRecommendationsNotifier
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.survey.repository.SurveyRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class SurveyModule(
    val surveyService: SurveyService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            personalityCoordinator: PersonalitySurveyCoordinator,
        ): SurveyModule {
            val surveyRepository = SurveyRepository()
            val surveyService =
                SurveyService(
                    auth.seekerRepository,
                    surveyRepository,
                    personalityCoordinator,
                )
            return SurveyModule(surveyService = surveyService)
        }
    }
}

data class MatchingModule(
    val matchingService: CachedMatchingService,
    val matchInterestService: MatchInterestService,
    val matchInterestNotifier: RedisMatchInterestNotifier,
    val recommendationsEventService: RecommendationsEventService,
    val recommendationsNotifier: RedisRecommendationsNotifier,
    val cacheInvalidator: MatchingCacheInvalidator,
    private val matchResultsConsumer: MatchResultsConsumer,
) {
    fun close() {
        matchResultsConsumer.stop()
        recommendationsNotifier.close()
    }

    companion object {
        fun create(
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            config: AppConfig,
        ): MatchingModule {
            val matchingRepository = MatchingRepository(auth.referenceRepository)
            val matchScoreRepository = MatchScoreRepository()
            val matchInterestRepository = MatchInterestRepository()
            val cacheInvalidator = MatchingCacheInvalidator(redis.client, config.redis)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val matchInterestNotifier =
                RedisMatchInterestNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val recommendationsNotifier =
                RedisRecommendationsNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val matchingQueries =
                LocalMatchingQueries(
                    matchScoreRepository = matchScoreRepository,
                    matchingRepository = matchingRepository,
                    seekerRepository = auth.seekerRepository,
                    referenceRepository = auth.referenceRepository,
                )
            val matchingService =
                CachedMatchingService(
                    delegate = matchingQueries,
                    resolveSeekerId = { userId -> auth.seekerRepository.findByUserId(userId)?.id },
                    redis = redis.client,
                    config = config.redis,
                )
            val matchInterestService =
                MatchInterestService(
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    matchingService = matchingService,
                    matchingRepository = matchingRepository,
                    matchInterestRepository = matchInterestRepository,
                    surveyService = survey.surveyService,
                    notifier = matchInterestNotifier,
                )
            val recommendationsEventService =
                RecommendationsEventService(recommendationsNotifier)
            val applyService =
                MatchResultsApplyService(
                    matchScoreRepository = matchScoreRepository,
                    matchingRepository = matchingRepository,
                    cacheInvalidator = cacheInvalidator,
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    recommendationsNotifier = recommendationsNotifier,
                )
            val matchResultsConsumer =
                MatchResultsConsumer(
                    kafkaConfig = config.kafka,
                    applyService = applyService,
                    dedup =
                        MatchResultsEventDedup(
                            redis = redis.client,
                            config = config.redis,
                            kafkaConfig = config.kafka,
                        ),
                )
            redis.registerOnClose(matchInterestNotifier)
            redis.registerOnClose(recommendationsNotifier)
            matchInterestNotifier.start()
            recommendationsNotifier.start()
            matchResultsConsumer.start()
            return MatchingModule(
                matchingService = matchingService,
                matchInterestService = matchInterestService,
                matchInterestNotifier = matchInterestNotifier,
                recommendationsEventService = recommendationsEventService,
                recommendationsNotifier = recommendationsNotifier,
                cacheInvalidator = cacheInvalidator,
                matchResultsConsumer = matchResultsConsumer,
            )
        }
    }
}

data class SeekerModule(
    val seekerProfileService: SeekerProfileService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            matching: MatchingModule,
            survey: SurveyModule,
            matchingEvents: MatchingEventPort,
        ): SeekerModule {
            val seekerProfileService =
                SeekerProfileService(
                    auth.seekerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    survey.surveyService,
                    matching.cacheInvalidator,
                    matchingEvents,
                )
            return SeekerModule(seekerProfileService = seekerProfileService)
        }
    }
}

data class EmployerModule(
    val employerProfileService: EmployerProfileService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            matching: MatchingModule,
            matchingEvents: MatchingEventPort,
        ): EmployerModule {
            val employerProfileService =
                EmployerProfileService(
                    auth.employerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    matching.cacheInvalidator,
                    matchingEvents,
                )
            return EmployerModule(employerProfileService = employerProfileService)
        }
    }
}
