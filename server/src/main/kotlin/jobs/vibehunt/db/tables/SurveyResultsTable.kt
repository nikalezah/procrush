package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SurveyResultsTable : LongIdTable("survey_results") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE).nullable()
    val surveyId = reference("survey_id", SurveysTable, onDelete = ReferenceOption.CASCADE).nullable()
    val answers = text("answers")
    val calculatedResults = text("calculated_results").nullable()
    val startedAt = timestampWithTimeZone("started_at")
    val completedAt = timestampWithTimeZone("completed_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
}
