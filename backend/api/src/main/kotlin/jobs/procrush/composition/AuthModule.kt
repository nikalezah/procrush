package jobs.procrush.composition

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
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.config.toAuthAppConfig
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository

data class AuthModule(
    val userRepository: UserRepository,
    val sessionRepository: SessionStore,
    val seekerRepository: SeekerRepository,
    val referenceRepository: ReferenceRepository,
    val employerRepository: EmployerRepository,
    val profileProvisioningService: ProfileProvisioningService,
    val profileEnricher: UserProfileEnricher,
    val userAuthService: UserAuthService,
    val sessionService: SessionService,
    val roleGuard: RoleGuard,
) {
    companion object {
        fun create(config: AppConfig, redis: RedisModule): AuthModule = createInternal(config, redis)

        fun create(config: WorkerAppConfig, redis: RedisModule): AuthModule =
            createInternal(config.toAuthAppConfig(), redis)

        private fun createInternal(config: AppConfig, redis: RedisModule): AuthModule {
            val userRepository = UserRepository()
            val delegateSessionRepository = SessionRepository()
            val sessionRepository =
                CachingSessionRepository(
                    delegate = delegateSessionRepository,
                    redis = redis.client,
                    config = config.redis,
                )
            val seekerRepository = SeekerRepository()
            val referenceRepository = ReferenceRepository()
            val employerRepository = EmployerRepository(referenceRepository)
            val profileProvisioningService = ProfileProvisioningService(seekerRepository, employerRepository)
            val profileEnricher = UserProfileEnricher(seekerRepository, employerRepository)
            val userAuthService = UserAuthService(userRepository, profileProvisioningService, profileEnricher)
            val sessionService =
                SessionService(config, sessionRepository, userRepository, profileEnricher::enrich)
            val roleGuard = RoleGuard(config, sessionService)
            return AuthModule(
                userRepository = userRepository,
                sessionRepository = sessionRepository,
                seekerRepository = seekerRepository,
                referenceRepository = referenceRepository,
                employerRepository = employerRepository,
                profileProvisioningService = profileProvisioningService,
                profileEnricher = profileEnricher,
                userAuthService = userAuthService,
                sessionService = sessionService,
                roleGuard = roleGuard,
            )
        }
    }
}
