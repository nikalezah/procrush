package jobs.procrush.matching.runtime.model

import java.time.OffsetDateTime

data class StoredMatchResult(
    val seekerId: Long,
    val jobProfileId: Long,
    val matchScore: Double,
    val personalityIncluded: Boolean,
    val computedAt: OffsetDateTime,
)
