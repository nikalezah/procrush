package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.survey.dto.SurveyLlmContextDto
import jobs.procrush.survey.service.SurveyService
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
