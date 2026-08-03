package jobs.procrush.matching.model

import java.time.OffsetDateTime

data class StoredMatchScore(
    val seekerId: Long,
    val jobProfileId: Long,
    val matchScore: Double,
    val matchScoreDisplay: Int,
    val personalityIncluded: Boolean,
    val computedAt: OffsetDateTime,
)

data class MatchScoreApplyResult(
    val affectedSeekerIds: Set<Long>,
    val affectedJobProfileIds: Set<Long>,
    val computedAt: OffsetDateTime,
)
