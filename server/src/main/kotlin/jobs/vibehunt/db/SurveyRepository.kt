package jobs.procrush.db

import jobs.procrush.db.tables.GlossaryTermsTable
import jobs.procrush.db.tables.SurveyKeysTable
import jobs.procrush.db.tables.SurveyResultsTable
import jobs.procrush.db.tables.SurveysTable
import jobs.procrush.survey.GlossaryTermDto
import jobs.procrush.survey.SurveyDefinitionDto
import jobs.procrush.survey.SurveyKeyDto
import jobs.procrush.survey.SurveyResultDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime

class SurveyRepository {
    fun listSurveys(): List<SurveyDefinitionDto> =
        transaction {
            SurveysTable
                .selectAll()
                .orderBy(SurveysTable.groupCode to SortOrder.ASC, SurveysTable.sortOrder to SortOrder.ASC)
                .map { it.toSurveyDefinition() }
        }

    fun findSurveyById(id: Long): SurveyDefinitionDto? =
        transaction {
            SurveysTable.selectAll().where { SurveysTable.id eq id }.firstOrNull()?.toSurveyDefinition()
        }

    fun findSurveyKey(surveyId: Long): SurveyKeyDto? =
        transaction {
            SurveyKeysTable
                .selectAll()
                .where { SurveyKeysTable.surveyId eq surveyId }
                .firstOrNull()
                ?.let {
                    SurveyKeyDto(
                        surveyId = surveyId,
                        scoringLogic = it[SurveyKeysTable.scoringLogic],
                        keysDataJson = it[SurveyKeysTable.keysData],
                    )
                }
        }

    fun listResultsForSeeker(seekerId: Long): List<SurveyResultDto> =
        transaction {
            SurveyResultsTable
                .selectAll()
                .where { SurveyResultsTable.seekerId eq seekerId }
                .map { it.toSurveyResult() }
        }

    fun findInProgressResult(seekerId: Long, surveyId: Long): SurveyResultDto? =
        transaction {
            SurveyResultsTable
                .selectAll()
                .where {
                    (SurveyResultsTable.seekerId eq seekerId) and
                        (SurveyResultsTable.surveyId eq surveyId) and
                        SurveyResultsTable.completedAt.isNull()
                }
                .firstOrNull()
                ?.toSurveyResult()
        }

    fun findCompletedResult(seekerId: Long, surveyId: Long): SurveyResultDto? =
        transaction {
            SurveyResultsTable
                .selectAll()
                .where {
                    (SurveyResultsTable.seekerId eq seekerId) and
                        (SurveyResultsTable.surveyId eq surveyId) and
                        SurveyResultsTable.completedAt.isNotNull()
                }
                .firstOrNull()
                ?.toSurveyResult()
        }

    fun createInProgressResult(seekerId: Long, surveyId: Long): SurveyResultDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                SurveyResultsTable.insert {
                    it[SurveyResultsTable.seekerId] = seekerId
                    it[SurveyResultsTable.surveyId] = surveyId
                    it[answers] = "{}"
                    it[calculatedResults] = null
                    it[startedAt] = now
                    it[completedAt] = null
                    it[updatedAt] = now
                }[SurveyResultsTable.id].value
            findResultById(id)!!
        }

    fun updateAnswers(resultId: Long, answersJson: String): SurveyResultDto? =
        transaction {
            val updated =
                SurveyResultsTable.update({ SurveyResultsTable.id eq resultId }) {
                    it[answers] = answersJson
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            findResultById(resultId)
        }

    fun completeResult(resultId: Long, answersJson: String, calculatedResultsJson: String): SurveyResultDto? =
        transaction {
            val now = OffsetDateTime.now()
            val updated =
                SurveyResultsTable.update({ SurveyResultsTable.id eq resultId }) {
                    it[answers] = answersJson
                    it[calculatedResults] = calculatedResultsJson
                    it[completedAt] = now
                    it[updatedAt] = now
                }
            if (updated == 0) return@transaction null
            findResultById(resultId)
        }

    fun updateCompletedResult(resultId: Long, answersJson: String, calculatedResultsJson: String): SurveyResultDto? =
        transaction {
            val now = OffsetDateTime.now()
            val updated =
                SurveyResultsTable.update({ SurveyResultsTable.id eq resultId }) {
                    it[answers] = answersJson
                    it[calculatedResults] = calculatedResultsJson
                    it[updatedAt] = now
                }
            if (updated == 0) return@transaction null
            findResultById(resultId)
        }

    fun listGlossaryTerms(): List<GlossaryTermDto> =
        transaction {
            GlossaryTermsTable.selectAll().map {
                GlossaryTermDto(
                    id = it[GlossaryTermsTable.id].value,
                    term = it[GlossaryTermsTable.term],
                    definition = it[GlossaryTermsTable.definition],
                    description = it[GlossaryTermsTable.description],
                )
            }
        }

    private fun findResultById(id: Long): SurveyResultDto? =
        SurveyResultsTable.selectAll().where { SurveyResultsTable.id eq id }.firstOrNull()?.toSurveyResult()

    private fun ResultRow.toSurveyDefinition() =
        SurveyDefinitionDto(
            id = this[SurveysTable.id].value,
            code = this[SurveysTable.code],
            name = this[SurveysTable.name],
            description = this[SurveysTable.description],
            groupCode = this[SurveysTable.groupCode],
            sortOrder = this[SurveysTable.sortOrder],
            questionsJson = this[SurveysTable.questions],
        )

    private fun ResultRow.toSurveyResult() =
        SurveyResultDto(
            id = this[SurveyResultsTable.id].value,
            seekerId = this[SurveyResultsTable.seekerId]?.value ?: error("seeker_id required"),
            surveyId = this[SurveyResultsTable.surveyId]?.value ?: error("survey_id required"),
            answersJson = this[SurveyResultsTable.answers],
            calculatedResultsJson = this[SurveyResultsTable.calculatedResults],
            startedAt = this[SurveyResultsTable.startedAt].toString(),
            completedAt = this[SurveyResultsTable.completedAt]?.toString(),
        )
}
