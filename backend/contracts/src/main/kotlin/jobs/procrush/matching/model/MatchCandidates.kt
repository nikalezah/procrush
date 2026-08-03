package jobs.procrush.matching.model

import jobs.procrush.personality.dto.PersonalityAxesDto

data class JobMatchCandidate(
    val jobProfileId: Long,
    val employerId: Long,
    val companyName: String,
    val occupationId: Long,
    val occupationName: String,
    val description: String?,
    val isActive: Boolean,
    val skillIds: Set<Long>,
    val personalityAxes: PersonalityAxesDto,
)

/** Scoring-only seeker projection used by the matching service. */
data class SeekerMatchCandidate(
    val seekerId: Long,
    val occupationId: Long,
    val skillIds: Set<Long>,
    val personalityAxes: PersonalityAxesDto?,
    val personalityReady: Boolean,
)

data class SeekerMatchingContext(
    val skillIds: Set<Long>,
    val personalityAxes: PersonalityAxesDto?,
    val personalityReady: Boolean,
)
