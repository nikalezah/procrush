package jobs.procrush.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchInterestEventDto(
    val jobProfileId: Long,
    val seekerId: Long,
    val interestStatus: InterestStatus,
    val employerContact: EmployerContactDto? = null,
    val seekerContact: SeekerContactDto? = null,
)

@Serializable
data class MatchInterestCountDto(
    val count: Int,
)
