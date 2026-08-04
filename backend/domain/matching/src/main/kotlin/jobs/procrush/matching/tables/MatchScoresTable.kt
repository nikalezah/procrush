package jobs.procrush.matching.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object MatchScoresTable : Table("match_scores") {
    val seekerId = long("seeker_id")
    val jobProfileId = long("job_profile_id")
    val matchScore = double("match_score")
    val personalityIncluded = bool("personality_included")
    val computedAt = timestampWithTimeZone("computed_at")

    override val primaryKey = PrimaryKey(seekerId, jobProfileId)
}
