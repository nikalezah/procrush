package jobs.procrush.survey

import jobs.procrush.survey.SurveyStatus.COMPLETED

internal object SurveyFlowRules {
    const val CORE_GROUP = "core"
    const val GROUP_64QN = "64qn"

    fun isCoreGroupComplete(
        coreSurveys: List<SurveyDefinitionDto>,
        statusBySurvey: Map<Long, SurveyStatus>,
    ): Boolean =
        coreSurveys.isNotEmpty() &&
            coreSurveys.all { statusBySurvey[it.id] == COMPLETED }

    fun isSurveyLocked(
        survey: SurveyDefinitionDto,
        coreSurveys: List<SurveyDefinitionDto>,
        statusBySurvey: Map<Long, SurveyStatus>,
    ): Boolean =
        when (survey.groupCode) {
            CORE_GROUP ->
                coreSurveys
                    .filter { it.sortOrder < survey.sortOrder }
                    .any { statusBySurvey[it.id] != COMPLETED }
            GROUP_64QN -> !isCoreGroupComplete(coreSurveys, statusBySurvey)
            else -> false
        }

    fun nextSurveyInCore(
        completedSurvey: SurveyDefinitionDto,
        coreSurveys: List<SurveyDefinitionDto>,
    ): Long? {
        if (completedSurvey.groupCode != CORE_GROUP) return null
        return coreSurveys
            .filter { it.sortOrder > completedSurvey.sortOrder }
            .minByOrNull { it.sortOrder }
            ?.id
    }

    fun prevSurveyInCore(
        survey: SurveyDefinitionDto,
        coreSurveys: List<SurveyDefinitionDto>,
    ): Long? {
        if (survey.groupCode != CORE_GROUP) return null
        return coreSurveys
            .filter { it.sortOrder < survey.sortOrder }
            .maxByOrNull { it.sortOrder }
            ?.id
    }

    fun coreStepNumber(
        survey: SurveyDefinitionDto,
        coreSurveys: List<SurveyDefinitionDto>,
    ): Int? {
        if (survey.groupCode != CORE_GROUP) return null
        return coreSurveys.sortedBy { it.sortOrder }.indexOfFirst { it.id == survey.id }.let {
            if (it < 0) null else it + 1
        }
    }

    fun groupStatus(surveys: List<SurveyListItemDto>): SurveyStatus {
        if (surveys.isEmpty()) return SurveyStatus.NOT_STARTED
        val completed = surveys.count { it.status == COMPLETED }
        if (completed == surveys.size) return COMPLETED
        if (surveys.any { it.status == SurveyStatus.IN_PROGRESS || it.status == COMPLETED }) {
            return SurveyStatus.IN_PROGRESS
        }
        return SurveyStatus.NOT_STARTED
    }

    fun entrySurveyId(surveys: List<SurveyListItemDto>): Long? {
        surveys.firstOrNull { it.status == SurveyStatus.IN_PROGRESS && !it.locked }?.id?.let { return it }
        return surveys.firstOrNull { it.status != COMPLETED && !it.locked }?.id
    }

    fun isGroupLocked(
        groupCode: String,
        coreSurveys: List<SurveyDefinitionDto>,
        statusBySurvey: Map<Long, SurveyStatus>,
    ): Boolean =
        when (groupCode) {
            CORE_GROUP -> false
            GROUP_64QN -> !isCoreGroupComplete(coreSurveys, statusBySurvey)
            else -> false
        }
}
