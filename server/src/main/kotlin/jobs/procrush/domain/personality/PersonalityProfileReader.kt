package jobs.procrush.domain.personality

import jobs.procrush.db.SeekerPersonalProfileRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.domain.PersonalityProfileMapper
import jobs.procrush.domain.SurveyService
import jobs.procrush.models.PersonalityPreviewDto
import jobs.procrush.models.PersonalityProfileStatus
import java.util.UUID

class PersonalityProfileReader(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val surveyService: SurveyService,
    private val generator: PersonalityProfileGenerator,
) {
    fun getPreview(userId: UUID): PersonalityPreviewDto {
        val groups = surveyService.listGroups(userId)
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")

        if (groups.testsCompleted < groups.testsTotal) {
            return PersonalityPreviewDto(
                status = PersonalityProfileStatus.NOT_READY,
                testsCompleted = groups.testsCompleted,
                testsTotal = groups.testsTotal,
            )
        }

        val processingPreview =
            PersonalityPreviewDto(
                status = PersonalityProfileStatus.PROCESSING,
                testsCompleted = groups.testsCompleted,
                testsTotal = groups.testsTotal,
            )

        if (generator.isJobActive(seeker.id)) {
            return processingPreview
        }

        val current = profileRepository.findBySeekerId(seeker.id)
        if (current == null) {
            return processingPreview
        }

        return when (current.generationStatus) {
            PersonalityProfileStatus.READY ->
                PersonalityProfileMapper.toPreview(current, groups.testsCompleted, groups.testsTotal)
            PersonalityProfileStatus.FAILED ->
                PersonalityPreviewDto(
                    status = PersonalityProfileStatus.FAILED,
                    generationError = current.generationError ?: "Не удалось сформировать профиль",
                    testsCompleted = groups.testsCompleted,
                    testsTotal = groups.testsTotal,
                )
            PersonalityProfileStatus.PROCESSING,
            PersonalityProfileStatus.NOT_READY,
            -> processingPreview
        }
    }
}
