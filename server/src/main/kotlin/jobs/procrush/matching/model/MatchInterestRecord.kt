package jobs.procrush.matching.model

import java.time.OffsetDateTime

data class MatchInterestRecord(
    val seekerId: Long,
    val jobProfileId: Long,
    val seekerRespondedAt: OffsetDateTime?,
    val employerRespondedAt: OffsetDateTime?,
) {
    val isMutual: Boolean
        get() = seekerRespondedAt != null && employerRespondedAt != null

    val seekerResponded: Boolean
        get() = seekerRespondedAt != null

    val employerResponded: Boolean
        get() = employerRespondedAt != null
}
