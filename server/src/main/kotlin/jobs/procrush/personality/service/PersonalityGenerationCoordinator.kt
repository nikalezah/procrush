package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class PersonalityGenerationCoordinator(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val surveyService: SurveyService,
    private val generator: PersonalityProfileGenerator,
) {
    fun onAllSurveysCompleted(userId: UUID) {
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
                record.generationStatus == PersonalityProfileStatus.PROCESSING && generator.isStale(record) -> true
                else -> false
            }
        if (!shouldStart) return

        generator.startGeneration(seeker.id, userId)
    }

    fun triggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        require(groups.testsCompleted >= groups.testsTotal) { "Сначала пройдите все группы тестов" }

        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val record = profileRepository.findBySeekerId(seeker.id)
        if (record?.generationStatus == PersonalityProfileStatus.PROCESSING && !generator.isStale(record)) {
            throw GenerationInProgressException()
        }
        generator.startGeneration(seeker.id, userId)
    }
}
