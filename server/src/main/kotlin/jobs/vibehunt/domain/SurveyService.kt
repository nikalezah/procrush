package jobs.procrush.domain

import jobs.procrush.db.SeekerRepository
import jobs.procrush.db.SurveyRepository
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
    private var profileGenerationTrigger: ((UUID) -> Unit)? = null

    fun setProfileGenerationTrigger(trigger: (UUID) -> Unit) {
        profileGenerationTrigger = trigger
    }

    private val groupNames =
        mapOf(
            SurveyFlowRules.CORE_GROUP to "Тест 1",
            SurveyFlowRules.GROUP_64QN to "Тест 2",
        )

    private val groupOrder = listOf(SurveyFlowRules.CORE_GROUP, SurveyFlowRules.GROUP_64QN)

    fun listGroups(userId: UUID): SurveyGroupsResponseDto {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val surveys = surveyRepository.listSurveys()
        val results = surveyRepository.listResultsForSeeker(seeker.id)
        val statusBySurvey = buildStatusMap(results)
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }

        val groups =
            groupOrder.map { groupCode ->
                val groupSurveys = surveys.filter { it.groupCode == groupCode }
                val items =
                    groupSurveys.map { survey ->
                        SurveyListItemDto(
                            id = survey.id,
                            code = survey.code,
                            name = survey.name,
                            description = survey.description,
                            status = statusBySurvey[survey.id] ?: SurveyStatus.NOT_STARTED,
                            sortOrder = survey.sortOrder,
                            locked = SurveyFlowRules.isSurveyLocked(survey, coreSurveys, statusBySurvey),
                        )
                    }
                val completed = items.count { it.status == SurveyStatus.COMPLETED }
                val groupLocked = SurveyFlowRules.isGroupLocked(groupCode, coreSurveys, statusBySurvey)
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
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val status = resolveStatus(seeker.id, surveyId)
        val inProgress = surveyRepository.findInProgressResult(seeker.id, surveyId)
        val completed = surveyRepository.findCompletedResult(seeker.id, surveyId)
        val surveys = surveyRepository.listSurveys()
        val results = surveyRepository.listResultsForSeeker(seeker.id)
        val statusBySurvey = buildStatusMap(results)
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }.sortedBy { it.sortOrder }
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
            locked = SurveyFlowRules.isSurveyLocked(survey, coreSurveys, statusBySurvey),
            stepNumber = stepNumber,
            stepTotal = stepTotal,
            prevSurveyId = SurveyFlowRules.prevSurveyInCore(survey, coreSurveys),
            nextSurveyId = SurveyFlowRules.nextSurveyInCore(survey, coreSurveys),
        )
    }

    fun startSurvey(userId: UUID, surveyId: Long): SurveyDetailDto {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        if (surveyRepository.findCompletedResult(seeker.id, surveyId) != null) {
            error("Опрос уже пройден")
        }
        val surveys = surveyRepository.listSurveys()
        val results = surveyRepository.listResultsForSeeker(seeker.id)
        val statusBySurvey = buildStatusMap(results)
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }
        if (SurveyFlowRules.isSurveyLocked(survey, coreSurveys, statusBySurvey)) {
            error("Сначала завершите предыдущие методики")
        }
        if (surveyRepository.findInProgressResult(seeker.id, surveyId) == null) {
            surveyRepository.createInProgressResult(seeker.id, surveyId)
        }
        return getSurvey(userId, surveyId)
    }

    fun saveAnswers(userId: UUID, surveyId: Long, request: SaveSurveyAnswersRequest): SurveyDetailDto {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val answersJson = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), request.answers)
        val result =
            surveyRepository.findInProgressResult(seeker.id, surveyId)
                ?: editableCompletedResult(seeker.id, survey)
                ?: error("Сначала начните опрос")
        surveyRepository.updateAnswers(result.id, answersJson)
            ?: error("Не удалось сохранить ответы")
        return getSurvey(userId, surveyId)
    }

    fun completeSurvey(userId: UUID, surveyId: Long, request: SaveSurveyAnswersRequest): CompleteSurveyResult {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val survey = surveyRepository.findSurveyById(surveyId) ?: error("Опрос не найден")
        val surveys = surveyRepository.listSurveys()
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }.sortedBy { it.sortOrder }
        val completed = surveyRepository.findCompletedResult(seeker.id, surveyId)
        val coreGroupComplete =
            SurveyFlowRules.isCoreGroupComplete(
                coreSurveys,
                buildStatusMap(surveyRepository.listResultsForSeeker(seeker.id)),
            )

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
                completed != null -> error("Опрос уже пройден")
                else -> {
                    val result =
                        surveyRepository.findInProgressResult(seeker.id, surveyId)
                            ?: error("Сначала начните опрос")
                    surveyRepository.completeResult(result.id, answersJson, calculated)
                        ?: error("Не удалось завершить опрос")
                }
            }

        val groups = listGroups(userId)
        if (groups.testsCompleted >= groups.testsTotal) {
            profileGenerationTrigger?.invoke(userId)
        }
        val nextSurveyId = SurveyFlowRules.nextSurveyInCore(survey, coreSurveys)
        return CompleteSurveyResult(
            result = completedResult,
            nextSurveyId = nextSurveyId,
        )
    }

    fun buildLlmContext(userId: UUID): SurveyLlmContextDto {
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val surveys = surveyRepository.listSurveys().associateBy { it.id }
        val completed =
            surveyRepository.listResultsForSeeker(seeker.id)
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

    private fun editableCompletedResult(seekerId: Long, survey: SurveyDefinitionDto): SurveyResultDto? {
        if (survey.groupCode != SurveyFlowRules.CORE_GROUP) return null
        val completed = surveyRepository.findCompletedResult(seekerId, survey.id) ?: return null
        val surveys = surveyRepository.listSurveys()
        val coreSurveys = surveys.filter { it.groupCode == SurveyFlowRules.CORE_GROUP }
        val statusBySurvey = buildStatusMap(surveyRepository.listResultsForSeeker(seekerId))
        if (SurveyFlowRules.isCoreGroupComplete(coreSurveys, statusBySurvey)) return null
        return completed
    }
}

data class CompleteSurveyResult(
    val result: SurveyResultDto,
    val nextSurveyId: Long?,
)
