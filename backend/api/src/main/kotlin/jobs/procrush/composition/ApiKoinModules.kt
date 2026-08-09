package jobs.procrush.composition

import jobs.procrush.api.handler.ApiHandlers
import jobs.procrush.api.handler.AuthHandler
import jobs.procrush.api.handler.EmployerHandler
import jobs.procrush.api.handler.ReferenceHandler
import jobs.procrush.api.handler.SeekerPersonalityHandler
import jobs.procrush.api.handler.SeekerProfileHandler
import jobs.procrush.api.handler.SeekerSurveyHandler
import jobs.procrush.api.rabbitmq.RabbitMqModule
import jobs.procrush.auth.ProfileProvisioningService
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserAuthService
import jobs.procrush.auth.UserProfileEnricher
import jobs.procrush.auth.repository.CachingSessionRepository
import jobs.procrush.auth.repository.SessionRepository
import jobs.procrush.auth.repository.SessionStore
import jobs.procrush.auth.repository.UserRepository
import jobs.procrush.auth.service.SessionService
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.bootstrap.redis.RedisDistributedLock
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.cache.CachedMatchingService
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.kafka.MatchingEventsRuntime
import jobs.procrush.matching.messaging.MatchResultsConsumer
import jobs.procrush.matching.messaging.MatchResultsEventDedup
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.repository.MatchInterestRepository
import jobs.procrush.matching.repository.MatchScoreRepository
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.matching.service.LocalMatchingQueries
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.matching.service.MatchResultsApplyService
import jobs.procrush.matching.service.MatchingQueries
import jobs.procrush.matching.service.RecommendationsEventService
import jobs.procrush.matching.service.RedisMatchInterestNotifier
import jobs.procrush.matching.service.RedisRecommendationsNotifier
import jobs.procrush.personality.PersonalityRuntime
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.repository.SurveyRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun apiKoinModules(config: AppConfig): List<Module> =
    listOf(
        module {
            single { config }
            single { RedisModule.create(get<AppConfig>()) }
            single<RedisClient> { get<RedisModule>().client }
            single<RedisDistributedLock> { get<RedisModule>().distributedLock }
            single { RabbitMqModule.create(get<AppConfig>().rabbitMq) }
            single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
        },
        module {
            singleOf(::UserRepository)
            singleOf(::SessionRepository)
            single<SessionStore> { CachingSessionRepository(get(), get(), get<AppConfig>().redis) }
            singleOf(::SeekerRepository)
            single { ReferenceRepository() }
            singleOf(::EmployerRepository)
            singleOf(::ProfileProvisioningService)
            singleOf(::UserProfileEnricher)
            singleOf(::UserAuthService)
            single { SessionService(get(), get(), get(), get<UserProfileEnricher>()::enrich) }
            singleOf(::RoleGuard)
        },
        module {
            singleOf(::DeferredPersonalitySurveyCoordinator)
            single<PersonalitySurveyCoordinator> { get<DeferredPersonalitySurveyCoordinator>() }
            singleOf(::SurveyRepository)
            singleOf(::SurveyService)
        },
        module {
            single { MatchingRepository(get()) }
            singleOf(::MatchScoreRepository)
            singleOf(::MatchInterestRepository)
            singleOf(::MatchingCacheInvalidator) bind MatchingCachePort::class
            single { MatchingEventsRuntime.create(get<AppConfig>().kafka, get(), get()) }
            single<MatchingEventPort> { get<MatchingEventsRuntime>().eventPort }
            single { RedisMatchInterestNotifier(get(), get<AppConfig>().redis, get()) }
            single { RedisRecommendationsNotifier(get(), get<AppConfig>().redis, get()) }
            single<MatchingQueries> { LocalMatchingQueries(get(), get(), get(), get()) }
            single {
                val seekerRepository = get<SeekerRepository>()
                CachedMatchingService(
                    delegate = get<MatchingQueries>(),
                    resolveSeekerId = { userId -> seekerRepository.findByUserId(userId)?.id },
                    redis = get(),
                    config = get<AppConfig>().redis,
                )
            }
            single {
                MatchInterestService(
                    seekerRepository = get(),
                    employerRepository = get(),
                    matchingService = get(),
                    matchingRepository = get(),
                    matchInterestRepository = get(),
                    surveyService = get(),
                    notifier = get(),
                )
            }
            singleOf(::RecommendationsEventService)
            singleOf(::MatchResultsApplyService)
            single { MatchResultsEventDedup(get(), get<AppConfig>().redis, get<AppConfig>().kafka) }
            single { MatchResultsConsumer(get<AppConfig>().kafka, get(), get()) }
        },
        module {
            single {
                PersonalityRuntime.create(
                    redisConfig = get<AppConfig>().redis,
                    rabbitMqConfig = get<AppConfig>().rabbitMq,
                    distributedLock = get(),
                    redis = get(),
                    messagePublisher = get<RabbitMqModule>().publisher,
                    messageConsumer = get<RabbitMqModule>().createConsumer(),
                    seekerRepository = get(),
                    referenceRepository = get(),
                    surveyService = get(),
                    matchingCache = get(),
                    matchingEvents = get(),
                    scope = get(),
                )
            }
            single { get<PersonalityRuntime>().personalityProfileService }
            single {
                SeekerProfileService(
                    get(), get(), get(), get(), get(), get(), get(),
                )
            }
            single {
                EmployerProfileService(
                    get(), get(), get(), get(), get(), get(),
                )
            }
        },
        module {
            singleOf(::AuthHandler)
            singleOf(::ReferenceHandler)
            singleOf(::SeekerProfileHandler)
            singleOf(::SeekerSurveyHandler)
            singleOf(::SeekerPersonalityHandler)
            singleOf(::EmployerHandler)
            singleOf(::ApiHandlers)
            singleOf(::ApiRuntime)
        },
    )