package jobs.procrush.domain

import jobs.procrush.db.SeekerRepository
import jobs.procrush.db.SurveyRepository
import jobs.procrush.domain.personality.PersonalityGenerationCoordinator
import jobs.procrush.models.SeekerProfileDto
import jobs.procrush.survey.SaveSurveyAnswersRequest
import jobs.procrush.survey.SurveyDefinitionDto
import jobs.procrush.survey.SurveyDetailDto
import jobs.procrush.survey.SurveyFlowRules
import jobs.procrush.survey.SurveyGroupDto
import jobs.procrush.survey.SurveyGroupsResponseDto
import jobs.procrush.survey.SurveyListItemDto
import jobs.procrush.survey.SurveyLlmContextDto
import jobs.procrush.survey.SurveyLlmContextItemDto
import jobs.procrush.survey.SurveyResultDto
import jobs.procrush.survey.SurveyStatus
import jobs.procrush.survey.scoring.SurveyAnswerValidator
import jobs.procrush.survey.scoring.SurveyScoringService
import kotlinx.serialization.json.Json
import java.util.UUID

class SurveyService(
    private val seekerRepository: SeekerRepository,
    private val surveyRepository: SurveyRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var personalityCoordinator: PersonalityGenerationCoordinator? = null

    fun attachPersonalityCoordinator(coordinator: PersonalityGenerationCoordinator) {
        personalityCoordinator = coordinator
    }

    private val groupNames =
        mapOf(
            SurveyFlowRules.CORE_GROUP to "Тест 1",
            SurveyFlowRules.GROUP_64QN to "Тест 2",
        )

    private val groupOrder = listOf(SurveyFlowRules.CORE_GROUP, SurveyFlowRules.GROUP_64QN)

    private data class SurveyContext(
        val seeker: SeekerProfileDto,
        val surveys: List<SurveyDefinitionDto>,
        val results: List<SurveyResultDto>,
        val statusBySurvey: Map<Long, SurveyStatus>,
        val coreSurveys: List<SurveyDefinitionDto>,
    )

    private fun loadContext(userId: UUID): SurveyContext {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val surveys = surveyRepository.listSurveys()
        val results = surveyRepository.listResultsForSeeker(seeker.id)
        val statusBySurvey = buildStatusMap(results)
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }
        return SurveyContext(seeker, surveys, results, statusBySurvey, coreSurveys)
    }

    fun listGroups(userId: UUID): SurveyGroupsResponseDto {
        val context = loadContext(userId)

        val groups =
            groupOrder.map { groupCode ->
                val groupSurveys = context.surveys.filter { it.groupCode == groupCode }
                val items =
                    groupSurveys.map { survey ->
                        SurveyListItemDto(
                            id = survey.id,
                            code = survey.code,
                            name = survey.name,
                            description = survey.description,
                            status = context.statusBySurvey[survey.id] ?: SurveyStatus.NOT_STARTED,
                            sortOrder = survey.sortOrder,
                            locked = SurveyFlowRules.isSurveyLocked(survey, context.coreSurveys, context.statusBySurvey),
                        )
                    }
                val completed = items.count { it.status == SurveyStatus.COMPLETED }
                val groupLocked = SurveyFlowRules.isGroupLocked(groupCode, context.coreSurveys, context.statusBySurvey)
                SurveyGroupDto(
                    code = groupCode,
                    name = groupNames[groupCode] ?: groupCode,
                    surveys = items,
                    completedCount = completed,
                    totalCount = items.size,
                    status = SurveyFlowRules.groupStatus(items),
                    locked = groupLocked,
                    entrySurveyId =
                        if (groupLocked || completed == items.size) {
                            null
                        } else {
                            SurveyFlowRules.entrySurveyId(items)
                        },
                )
            }

        val testsCompleted = groups.count { it.completedCount == it.totalCount && it.totalCount > 0 }
        return SurveyGroupsResponseDto(
            groups = groups,
            testsCompleted = testsCompleted,
            testsTotal = groups.size,
        )
    }

    fun getSurvey(userId: UUID, surveyId: Long): SurveyDetailDto {
        val context = loadContext(userId)
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val status = resolveStatus(context.seeker.id, surveyId)
        val inProgress = surveyRepository.findInProgressResult(context.seeker.id, surveyId)
        val completed = surveyRepository.findCompletedResult(context.seeker.id, surveyId)
        val coreSurveys = context.coreSurveys.sortedBy { it.sortOrder }
        val stepNumber = SurveyFlowRules.coreStepNumber(survey, coreSurveys)
        val stepTotal = if (survey.groupCode == SurveyFlowRules.CORE_GROUP) coreSurveys.size else null
        return SurveyDetailDto(
            id = survey.id,
            code = survey.code,
            name = survey.name,
            description = survey.description,
            groupCode = survey.groupCode,
            questionsJson = survey.questionsJson,
            status = status,
            answersJson = inProgress?.answersJson ?: completed?.answersJson,
            resultId = inProgress?.id ?: completed?.id,
            locked = SurveyFlowRules.isSurveyLocked(survey, context.coreSurveys, context.statusBySurvey),
            stepNumber = stepNumber,
            stepTotal = stepTotal,
            prevSurveyId = SurveyFlowRules.prevSurveyInCore(survey, coreSurveys),
            nextSurveyId = SurveyFlowRules.nextSurveyInCore(survey, coreSurveys),
        )
    }

    fun startSurvey(userId: UUID, surveyId: Long): SurveyDetailDto {
        val context = loadContext(userId)
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        if (surveyRepository.findCompletedResult(context.seeker.id, surveyId) != null) {
            throw SurveyAlreadyCompletedException()
        }
        if (SurveyFlowRules.isSurveyLocked(survey, context.coreSurveys, context.statusBySurvey)) {
            error("Сначала завершите предыдущие методики")
        }
        if (surveyRepository.findInProgressResult(context.seeker.id, surveyId) == null) {
            surveyRepository.createInProgressResult(context.seeker.id, surveyId)
        }
        return getSurvey(userId, surveyId)
    }

    fun saveAnswers(userId: UUID, surveyId: Long, request: SaveSurveyAnswersRequest): SurveyDetailDto {
        val context = loadContext(userId)
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val answersJson = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), request.answers)
        val result =
            surveyRepository.findInProgressResult(context.seeker.id, surveyId)
                ?: editableCompletedResult(context, survey)
                ?: error("Сначала начните опрос")
        surveyRepository.updateAnswers(result.id, answersJson)
            ?: error("Не удалось сохранить ответы")
        return getSurvey(userId, surveyId)
    }

    fun completeSurvey(userId: UUID, surveyId: Long, request: SaveSurveyAnswersRequest): CompleteSurveyResult {
        val context = loadContext(userId)
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val coreSurveys = context.coreSurveys.sortedBy { it.sortOrder }
        val completed = surveyRepository.findCompletedResult(context.seeker.id, surveyId)
        val coreGroupComplete = SurveyFlowRules.isCoreGroupComplete(coreSurveys, context.statusBySurvey)

        SurveyAnswerValidator.validate(survey.code, survey.questionsJson, request.answers)
        val answersJson = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), request.answers)
        val key = surveyRepository.findSurveyKey(surveyId) ?: error("Ключи подсчёта не найдены")
        val calculated =
            SurveyScoringService.calculate(
                surveyCode = survey.code,
                scoringLogic = key.scoringLogic,
                keysDataJson = key.keysDataJson,
                questionsJson = survey.questionsJson,
                answersJson = answersJson,
            )

        val completedResult =
            when {
                completed != null && survey.groupCode == SurveyFlowRules.CORE_GROUP && !coreGroupComplete -> {
                    surveyRepository.updateCompletedResult(completed.id, answersJson, calculated)
                        ?: error("Не удалось сохранить ответы")
                }
                completed != null -> throw SurveyAlreadyCompletedException()
                else -> {
                    val result =
                        surveyRepository.findInProgressResult(context.seeker.id, surveyId)
                            ?: error("Сначала начните опрос")
                    surveyRepository.completeResult(result.id, answersJson, calculated)
                        ?: error("Не удалось завершить опрос")
                }
            }

        val groups = listGroups(userId)
        if (groups.testsCompleted >= groups.testsTotal) {
            personalityCoordinator?.onAllSurveysCompleted(userId)
        }
        val nextSurveyId = SurveyFlowRules.nextSurveyInCore(survey, coreSurveys)
        return CompleteSurveyResult(
            result = completedResult,
            nextSurveyId = nextSurveyId,
        )
    }

    fun buildLlmContext(userId: UUID): SurveyLlmContextDto {
        val context = loadContext(userId)
        val surveys = context.surveys.associateBy { it.id }
        val completed =
            context.results
                .filter { it.completedAt != null && it.calculatedResultsJson != null }
        val items =
            completed.mapNotNull { result ->
                val survey = surveys[result.surveyId] ?: return@mapNotNull null
                SurveyLlmContextItemDto(
                    surveyCode = survey.code,
                    answersJson = result.answersJson,
                    calculatedResultsJson = result.calculatedResultsJson!!,
                )
            }
        return SurveyLlmContextDto(
            surveys = items,
            glossaryTerms = surveyRepository.listGlossaryTerms(),
        )
    }

    private fun buildStatusMap(results: List<SurveyResultDto>): Map<Long, SurveyStatus> {
        val map = mutableMapOf<Long, SurveyStatus>()
        results.forEach { result ->
            map[result.surveyId] =
                when {
                    result.completedAt != null -> SurveyStatus.COMPLETED
                    else -> SurveyStatus.IN_PROGRESS
                }
        }
        return map
    }

    private fun resolveStatus(seekerId: Long, surveyId: Long): SurveyStatus {
        val completed = surveyRepository.findCompletedResult(seekerId, surveyId)
        if (completed != null) return SurveyStatus.COMPLETED
        val inProgress = surveyRepository.findInProgressResult(seekerId, surveyId)
        if (inProgress != null) return SurveyStatus.IN_PROGRESS
        return SurveyStatus.NOT_STARTED
    }

    private fun editableCompletedResult(context: SurveyContext, survey: SurveyDefinitionDto): SurveyResultDto? {
        if (survey.groupCode != SurveyFlowRules.CORE_GROUP) return null
        val completed = surveyRepository.findCompletedResult(context.seeker.id, survey.id) ?: return null
        if (SurveyFlowRules.isCoreGroupComplete(context.coreSurveys, context.statusBySurvey)) return null
        return completed
    }
}

data class CompleteSurveyResult(
    val result: SurveyResultDto,
    val nextSurveyId: Long?,
)
