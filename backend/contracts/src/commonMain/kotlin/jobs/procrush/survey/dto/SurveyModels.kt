package jobs.procrush.survey.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

data class SurveyDefinitionDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String,
    val groupCode: String,
    val sortOrder: Int,
    val questionsJson: String,
)

data class SurveyKeyDto(
    val surveyId: Long,
    val scoringLogic: String,
    val keysDataJson: String,
)

data class SurveyResultDto(
    val id: Long,
    val seekerId: Long,
    val surveyId: Long,
    val answersJson: String,
    val calculatedResultsJson: String?,
    val startedAt: String,
    val completedAt: String?,
)

@Serializable
enum class SurveyStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

@Serializable
data class SurveyListItemDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String,
    val status: SurveyStatus,
    val sortOrder: Int,
    val locked: Boolean = false,
)

@Serializable
data class SurveyGroupDto(
    val code: String,
    val name: String,
    val surveys: List<SurveyListItemDto>,
    val completedCount: Int,
    val totalCount: Int,
    val status: SurveyStatus,
    val locked: Boolean = false,
    val entrySurveyId: Long? = null,
)

@Serializable
data class SurveyGroupsResponseDto(
    val groups: List<SurveyGroupDto>,
    val testsCompleted: Int,
    val testsTotal: Int,
)

@Serializable
data class SurveyDetailDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String,
    val groupCode: String,
    val questionsJson: String,
    val status: SurveyStatus,
    val answersJson: String?,
    val resultId: Long?,
    val locked: Boolean = false,
    val stepNumber: Int? = null,
    val stepTotal: Int? = null,
    val prevSurveyId: Long? = null,
    val nextSurveyId: Long? = null,
)

@Serializable
data class SaveSurveyAnswersRequest(
    val answers: JsonElement,
)

@Serializable
data class GlossaryTermDto(
    val id: Long,
    val term: String,
    val definition: String,
    val description: String,
)

@Serializable
data class SurveyLlmContextItemDto(
    val surveyCode: String,
    val answersJson: String,
    val calculatedResultsJson: String,
)

@Serializable
data class SurveyLlmContextDto(
    val surveys: List<SurveyLlmContextItemDto>,
    val glossaryTerms: List<GlossaryTermDto>,
)

@Serializable
data class CompleteSurveyResponseDto(
    val resultId: Long,
    val surveyId: Long,
    val status: SurveyStatus,
    val nextSurveyId: Long? = null,
)
