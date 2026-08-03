package jobs.procrush.personality.messaging

import jobs.procrush.shared.dto.SuperpowerAndTalentDto
import jobs.procrush.survey.dto.SurveyLlmContextDto
import kotlinx.serialization.Serializable

@Serializable
data class PersonalityGenerationCommand(
    val seekerId: Long,
    val userId: String,
    val surveyContext: SurveyLlmContextDto,
    val catalog: List<SuperpowerAndTalentDto>,
    val enqueuedAt: String,
    val attempt: Int = 1,
    val correlationId: String? = null,
)
