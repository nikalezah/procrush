package jobs.procrush.personality.service

import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.messaging.PersonalityJobPublisher
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class PersonalityGenerationCoordinator(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val surveyService: SurveyService,
    private val lockGuard: PersonalityGenerationLockGuard,
    private val publisher: PersonalityJobPublisher,
    private val matchingCacheInvalidator: MatchingCacheInvalidator,
    private val matchingEvents: MatchingEventPort,
) : PersonalitySurveyCoordinator {
    override fun onAllSurveysCompleted(userId: UUID) {
        seekerRepository.findByUserId(userId)?.let { seeker ->
            matchingCacheInvalidator.invalidateSeekerJobs(seeker.id)
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

        enqueueGeneration(seeker.id, userId)
    }

    fun triggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        require(groups.testsCompleted >= groups.testsTotal) { "Сначала пройдите все группы тестов" }

        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val record = profileRepository.findBySeekerId(seeker.id)
        if (record?.generationStatus == PersonalityProfileStatus.PROCESSING && !lockGuard.isStale(record)) {
            throw GenerationInProgressException()
        }
        enqueueGeneration(seeker.id, userId)
    }

    private fun enqueueGeneration(seekerId: Long, userId: UUID) {
        profileRepository.markProcessing(seekerId)
        publisher.enqueue(seekerId, userId)
    }
}
