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
data class JobCardDto(
    val id: Long,
    val companyName: String? = null,
    val positionName: String,
    val description: String,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: EmployerContactDto? = null,
)

@Serializable
data class JobRecommendationDto(
    val id: Long,
    val companyName: String? = null,
    val positionName: String,
    val description: String,
    val matchScore: Double,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: EmployerContactDto? = null,
)

@Serializable
data class CandidateCardDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val positionName: String,
    val skills: List<String>,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: SeekerContactDto? = null,
)

@Serializable
data class CandidateRecommendationDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val positionName: String,
    val skills: List<String>,
    val matchScore: Double,
    val interestStatus: InterestStatus = InterestStatus.NONE,
    val contactInfo: SeekerContactDto? = null,
)

@Serializable
data class SeekerInterestsResponseDto(
    val respondedOutside: List<JobCardDto>,
    val mutualOutside: List<JobCardDto>,
)

@Serializable
data class EmployerInterestsResponseDto(
    val respondedOutside: List<CandidateCardDto>,
    val mutualOutside: List<CandidateCardDto>,
)

@Serializable
data class EmployerCandidatesOverviewDto(
    val candidates: List<CandidateRecommendationDto>,
    val interests: EmployerInterestsResponseDto,
)

fun JobRecommendationDto.toCard(): JobCardDto =
    JobCardDto(
        id = id,
        companyName = companyName,
        positionName = positionName,
        description = description,
        interestStatus = interestStatus,
        contactInfo = contactInfo,
    )

fun CandidateRecommendationDto.toCard(): CandidateCardDto =
    CandidateCardDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        positionName = positionName,
        skills = skills,
        interestStatus = interestStatus,
        contactInfo = contactInfo,
    )
