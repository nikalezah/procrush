package jobs.procrush.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmployerContactDto(
    val companyName: String? = null,
    val phone: String? = null,
    val emailContact: String? = null,
    val website: String? = null,
)

@Serializable
data class SeekerContactDto(
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val telegram: String? = null,
    val linkedin: String? = null,
)

@Serializable
data class JobRecommendationDto(
    val id: Long,
    val companyName: String? = null,
    val positionName: String,
    val description: String,
    val matchScore: Double,
    val matchScoreDisplay: Int,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: EmployerContactDto? = null,
)

@Serializable
data class CandidateRecommendationDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val positionName: String,
    val skills: List<String>,
    val matchScore: Double,
    val matchScoreDisplay: Int,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: SeekerContactDto? = null,
)

@Serializable
data class SeekerInterestsResponseDto(
    val respondedOutside: List<JobRecommendationDto>,
    val mutualOutside: List<JobRecommendationDto>,
)

@Serializable
data class EmployerInterestsResponseDto(
    val respondedOutside: List<CandidateRecommendationDto>,
    val mutualOutside: List<CandidateRecommendationDto>,
)

@Serializable
data class EmployerCandidatesOverviewDto(
    val candidates: List<CandidateRecommendationDto>,
    val interests: EmployerInterestsResponseDto,
)
