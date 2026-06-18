package jobs.procrush.survey.scoring

import jobs.procrush.survey.dto.SurveyDefinitionDto
import jobs.procrush.survey.dto.SurveyListItemDto
import jobs.procrush.survey.dto.SurveyStatus
import jobs.procrush.survey.dto.SurveyStatus.COMPLETED
import jobs.procrush.survey.dto.SurveyStatus.NOT_STARTED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveyFlowRulesTest {
    private val coreSurveys =
        listOf(
            survey(id = 1, code = "2501-10-5KEY", group = SurveyFlowRules.CORE_GROUP, sort = 1),
            survey(id = 2, code = "2520-10-ETYA", group = SurveyFlowRules.CORE_GROUP, sort = 2),
            survey(id = 3, code = "2521-10-NEYA", group = SurveyFlowRules.CORE_GROUP, sort = 3),
        )

    private val q64 = survey(id = 10, code = "2540-10-64QN", group = SurveyFlowRules.GROUP_64QN, sort = 1)

    @Test
    fun firstCoreSurveyIsNotLocked() {
        val status = mapOf<Long, SurveyStatus>()
        assertFalse(SurveyFlowRules.isSurveyLocked(coreSurveys[0], coreSurveys, status))
    }

    @Test
    fun secondCoreSurveyLockedUntilFirstCompleted() {
        val status = mapOf(1L to NOT_STARTED)
        assertTrue(SurveyFlowRules.isSurveyLocked(coreSurveys[1], coreSurveys, status))

        val unlocked = mapOf(1L to COMPLETED)
        assertFalse(SurveyFlowRules.isSurveyLocked(coreSurveys[1], coreSurveys, unlocked))
    }

    @Test
    fun q64LockedUntilCoreComplete() {
        val partial = mapOf(1L to COMPLETED, 2L to COMPLETED, 3L to NOT_STARTED)
        assertTrue(SurveyFlowRules.isSurveyLocked(q64, coreSurveys, partial))

        val complete = mapOf(1L to COMPLETED, 2L to COMPLETED, 3L to COMPLETED)
        assertFalse(SurveyFlowRules.isSurveyLocked(q64, coreSurveys, complete))
    }

    @Test
    fun nextSurveyInCoreReturnsFollowingSurvey() {
        val next = SurveyFlowRules.nextSurveyInCore(coreSurveys[0], coreSurveys)
        assertEquals(2L, next)
    }

    @Test
    fun nextSurveyInCoreIsNullForLastSurvey() {
        val next = SurveyFlowRules.nextSurveyInCore(coreSurveys[2], coreSurveys)
        assertNull(next)
    }

    @Test
    fun nextSurveyInCoreIsNullForNonCoreSurvey() {
        val next = SurveyFlowRules.nextSurveyInCore(q64, coreSurveys)
        assertNull(next)
    }

    @Test
    fun prevSurveyInCoreReturnsPreviousSurvey() {
        assertNull(SurveyFlowRules.prevSurveyInCore(coreSurveys[0], coreSurveys))
        assertEquals(1L, SurveyFlowRules.prevSurveyInCore(coreSurveys[1], coreSurveys))
    }

    @Test
    fun coreStepNumberReflectsOrder() {
        assertEquals(1, SurveyFlowRules.coreStepNumber(coreSurveys[0], coreSurveys))
        assertEquals(2, SurveyFlowRules.coreStepNumber(coreSurveys[1], coreSurveys))
    }

    @Test
    fun isCoreGroupCompleteRequiresAllCoreSurveys() {
        assertFalse(
            SurveyFlowRules.isCoreGroupComplete(
                coreSurveys,
                mapOf(1L to COMPLETED, 2L to COMPLETED),
            ),
        )
        assertTrue(
            SurveyFlowRules.isCoreGroupComplete(
                coreSurveys,
                mapOf(1L to COMPLETED, 2L to COMPLETED, 3L to COMPLETED),
            ),
        )
    }

    @Test
    fun groupStatusReflectsProgress() {
        val items =
            coreSurveys.map { survey ->
                SurveyListItemDto(
                    id = survey.id,
                    code = survey.code,
                    name = survey.name,
                    description = survey.description,
                    status = NOT_STARTED,
                    sortOrder = survey.sortOrder,
                )
            }
        assertEquals(SurveyStatus.NOT_STARTED, SurveyFlowRules.groupStatus(items))

        val inProgress =
            items.map {
                if (it.id == 1L) it.copy(status = COMPLETED) else it
            }
        assertEquals(SurveyStatus.IN_PROGRESS, SurveyFlowRules.groupStatus(inProgress))

        val completed = items.map { it.copy(status = COMPLETED) }
        assertEquals(SurveyStatus.COMPLETED, SurveyFlowRules.groupStatus(completed))
    }

    @Test
    fun entrySurveyIdReturnsInProgressOrFirstAvailable() {
        val items =
            coreSurveys.map { survey ->
                SurveyListItemDto(
                    id = survey.id,
                    code = survey.code,
                    name = survey.name,
                    description = survey.description,
                    status = if (survey.id == 1L) COMPLETED else NOT_STARTED,
                    sortOrder = survey.sortOrder,
                )
            }
        assertEquals(2L, SurveyFlowRules.entrySurveyId(items))

        val withInProgress =
            items.map {
                if (it.id == 3L) it.copy(status = SurveyStatus.IN_PROGRESS) else it
            }
        assertEquals(3L, SurveyFlowRules.entrySurveyId(withInProgress))
    }

    @Test
    fun isGroupLockedOnlyFor64qnUntilCoreComplete() {
        val incomplete = mapOf(1L to COMPLETED, 2L to COMPLETED, 3L to NOT_STARTED)
        assertFalse(SurveyFlowRules.isGroupLocked(SurveyFlowRules.CORE_GROUP, coreSurveys, incomplete))
        assertTrue(SurveyFlowRules.isGroupLocked(SurveyFlowRules.GROUP_64QN, coreSurveys, incomplete))

        val complete = mapOf(1L to COMPLETED, 2L to COMPLETED, 3L to COMPLETED)
        assertFalse(SurveyFlowRules.isGroupLocked(SurveyFlowRules.GROUP_64QN, coreSurveys, complete))
    }

    private fun survey(
        id: Long,
        code: String,
        group: String,
        sort: Int,
    ) = SurveyDefinitionDto(
        id = id,
        code = code,
        name = code,
        description = "",
        groupCode = group,
        sortOrder = sort,
        questionsJson = "{}",
    )
}
