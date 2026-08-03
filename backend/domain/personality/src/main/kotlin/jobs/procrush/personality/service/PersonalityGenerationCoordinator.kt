package jobs.procrush.personality.service

import jobs.procrush.i18n.ErrorCode
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.messaging.PersonalityCommandPublisher
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.shared.raise
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityGenerationCoordinator(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val referenceRepository: ReferenceRepository,
    private val surveyService: SurveyService,
    private val lockGuard: PersonalityGenerationLockGuard,
    private val publisher: PersonalityCommandPublisher,
    private val matchingCache: MatchingCachePort,
    private val matchingEvents: MatchingEventPort,
) : PersonalitySurveyCoordinator {
    private val logger = LoggerFactory.getLogger(PersonalityGenerationCoordinator::class.java)

    override fun onAllSurveysCompleted(userId: UUID) {
        seekerRepository.findByUserId(userId)?.let { seeker ->
            matchingCache.invalidateSeekerJobs(seeker.id)
            matchingEvents.publishSeekerProfileChanged(seeker.id)
        }
        maybeTriggerGeneration(userId)
    }

    fun maybeTriggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        if (groups.testsCompleted < groups.testsTotal) return

        val seeker = seekerRepository.findByUserId(userId) ?: return
        val record = profileRepository.findBySeekerId(seeker.id)

        val shouldStart =
            when {
                record == null -> true
                record.generationStatus == PersonalityProfileStatus.PROCESSING && lockGuard.isStale(record) -> true
                else -> false
            }
        if (!shouldStart) return

        runCatching { enqueueGeneration(seeker.id, userId) }
            .onFailure { error ->
                if (error !is GenerationInProgressException) {
                    logger.warn("Failed to enqueue personality generation userId={}", userId, error)
                }
            }
    }

    fun triggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        if (groups.testsCompleted < groups.testsTotal) {
            ErrorCode.PERSONALITY_TESTS_NOT_COMPLETED.raise()
        }

        val seeker = seekerRepository.findByUserId(userId) ?: ErrorCode.SEEKER_NOT_FOUND.raise()
        val record = profileRepository.findBySeekerId(seeker.id)
        if (record?.generationStatus == PersonalityProfileStatus.PROCESSING && !lockGuard.isStale(record)) {
            throw GenerationInProgressException()
        }
        enqueueGeneration(seeker.id, userId)
    }

    private fun enqueueGeneration(seekerId: Long, userId: UUID) {
        if (!lockGuard.tryAcquire(seekerId)) {
            throw GenerationInProgressException()
        }
        try {
            val surveyContext = surveyService.buildLlmContext(userId)
            if (surveyContext.surveys.isEmpty()) {
                ErrorCode.NO_COMPLETED_SURVEYS.raise()
            }
            val catalog = referenceRepository.listSuperpowersAndTalents()
            profileRepository.markProcessing(seekerId)
            publisher.enqueue(
                seekerId = seekerId,
                userId = userId,
                surveyContext = surveyContext,
                catalog = catalog,
            )
        } catch (error: Exception) {
            lockGuard.release(seekerId)
            throw error
        }
    }
}
