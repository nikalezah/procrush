package jobs.procrush.matchingsvc.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object MatchResultsTable : Table("match_results") {
    val seekerId = long("seeker_id")
    val jobProfileId = long("job_profile_id")
    val occupationId = long("occupation_id")
    val companyName = text("company_name")
    val positionName = text("position_name")
    val jobDescription = text("job_description")
    val seekerFirstName = text("seeker_first_name")
    val seekerLastName = text("seeker_last_name")
    val seekerSkillsJson = text("seeker_skills_json")
    val matchScore = double("match_score")
    val matchScoreDisplay = integer("match_score_display")
    val personalityIncluded = bool("personality_included")
    val computedAt = timestampWithTimeZone("computed_at")

    override val primaryKey = PrimaryKey(seekerId, jobProfileId)
}
