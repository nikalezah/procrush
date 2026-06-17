package jobs.procrush.domain.personality

import jobs.procrush.domain.SurveyService
import jobs.procrush.models.PersonalityPreviewDto
import jobs.procrush.survey.SurveyLlmContextDto
import java.util.UUID

class PersonalityProfileService(
    private val reader: PersonalityProfileReader,
    private val coordinator: PersonalityGenerationCoordinator,
    private val surveyService: SurveyService,
) {
    fun getPreview(userId: UUID): PersonalityPreviewDto = reader.getPreview(userId)

    fun triggerGeneration(userId: UUID) = coordinator.triggerGeneration(userId)

    fun buildLlmContext(userId: UUID): SurveyLlmContextDto = surveyService.buildLlmContext(userId)
}
