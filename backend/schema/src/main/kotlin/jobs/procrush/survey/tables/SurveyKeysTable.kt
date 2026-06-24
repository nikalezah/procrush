package jobs.procrush.survey.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SurveyKeysTable : LongIdTable("survey_keys") {
    val surveyId = reference("survey_id", SurveysTable, onDelete = ReferenceOption.CASCADE).nullable()
    val scoringLogic = varchar("scoring_logic", 100)
    val keysData = text("keys_data")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
