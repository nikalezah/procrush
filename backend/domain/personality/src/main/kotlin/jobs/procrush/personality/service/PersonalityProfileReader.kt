package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.llm.PersonalityProfilePreviewMapper
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.seeker.repository.SeekerSuperpowersAndTalentsRepository
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class PersonalityProfileReader(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val superpowersRepository: SeekerSuperpowersAndTalentsRepository,
    private val surveyService: SurveyService,
    private val lockGuard: PersonalityGenerationLockGuard,
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

        if (lockGuard.isJobActive(seeker.id)) {
            return processingPreview
        }

        val current = profileRepository.findBySeekerId(seeker.id)
        if (current == null) {
            return processingPreview
        }

        return when (current.generationStatus) {
            PersonalityProfileStatus.READY -> {
                val superpowers = superpowersRepository.findBySeekerId(seeker.id)
                PersonalityProfilePreviewMapper.toPreview(
                    current,
                    groups.testsCompleted,
                    groups.testsTotal,
                    superpowers,
                )
            }
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
