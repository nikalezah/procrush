package jobs.procrush.matching.runtime.model

import java.time.OffsetDateTime

data class StoredMatchResult(
    val seekerId: Long,
    val jobProfileId: Long,
    val occupationId: Long,
    val companyName: String,
    val positionName: String,
    val jobDescription: String,
    val seekerFirstName: String,
    val seekerLastName: String,
    val seekerSkillsJson: String,
    val matchScore: Double,
    val matchScoreDisplay: Int,
    val personalityIncluded: Boolean,
    val computedAt: OffsetDateTime,
)
