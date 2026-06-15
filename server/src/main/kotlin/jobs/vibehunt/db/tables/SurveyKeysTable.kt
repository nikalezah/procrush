package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SurveyKeysTable : LongIdTable("survey_keys") {
    val surveyId = reference("survey_id", SurveysTable, onDelete = ReferenceOption.CASCADE).nullable()
    val scoringLogic = varchar("scoring_logic", 100)
    val keysData = text("keys_data")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
