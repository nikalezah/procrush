package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.survey.dto.SurveyLlmContextDto
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class PersonalityProfileService(
    private val reader: PersonalityProfileReader,
    private val coordinator: PersonalityGenerationCoordinator,
    private val surveyService: SurveyService,
    private val notifier: PersonalityGenerationNotifier,
) {
    fun getPreview(userId: UUID): PersonalityPreviewDto = reader.getPreview(userId)

    fun triggerGeneration(userId: UUID) = coordinator.triggerGeneration(userId)

    fun buildLlmContext(userId: UUID): SurveyLlmContextDto = surveyService.buildLlmContext(userId)

    suspend fun streamStatusEvents(
        userId: UUID,
        onStatus: suspend (PersonalityProfileStatus) -> Unit,
    ) {
        val preview = reader.getPreview(userId)
        if (preview.status != PersonalityProfileStatus.PROCESSING) {
            onStatus(preview.status)
            return
        }

        coordinator.maybeTriggerGeneration(userId)

        val deferred = notifier.register(userId)
        try {
            onStatus(deferred.await())
        } finally {
            notifier.cancel(userId, deferred)
        }
    }
}
