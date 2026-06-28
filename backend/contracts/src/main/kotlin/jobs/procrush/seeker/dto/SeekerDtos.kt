package jobs.procrush.seeker.dto

// Internal domain transport types. Public HTTP contract: openapi/specs/.
// API handlers map Spektor-generated DTOs ↔ these types via api/mapper/ApiMappers.kt.

import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.dto.SeekerInterestsResponseDto
import jobs.procrush.shared.dto.OccupationDto
import jobs.procrush.shared.dto.SkillDto
import kotlinx.serialization.Serializable

@Serializable
data class SeekerProfileDto(
    val id: Long,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val phone: String?,
    val telegram: String?,
    val linkedin: String?,
)

@Serializable
data class UpdateSeekerProfileRequest(
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val phone: String? = null,
    val telegram: String? = null,
    val linkedin: String? = null,
)

@Serializable
data class SeekerExperienceDto(
    val id: Long,
    val companyName: String,
    val position: String,
    val description: String?,
    val startDate: String,
    val endDate: String?,
)

@Serializable
data class CreateSeekerExperienceRequest(
    val companyName: String,
    val position: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
)

@Serializable
data class UpdateSeekerExperienceRequest(
    val companyName: String,
    val position: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
)

@Serializable
data class SeekerEducationDto(
    val id: Long,
    val institution: String,
    val degree: String?,
    val specialization: String,
    val endYear: Int,
)

@Serializable
data class CreateSeekerEducationRequest(
    val institution: String,
    val degree: String? = null,
    val specialization: String,
    val endYear: Int,
)

@Serializable
data class UpdateSeekerEducationRequest(
    val institution: String,
    val degree: String? = null,
    val specialization: String,
    val endYear: Int,
)

@Serializable
data class SeekerSkillsResponse(
    val skillIds: List<Long>,
    val skills: List<SkillDto>,
)

@Serializable
data class UpdateSeekerSkillsRequest(
    val skillIds: List<Long>,
)

@Serializable
data class SeekerDesiredPositionsResponse(
    val occupationIds: List<Long>,
    val occupations: List<OccupationDto>,
)

@Serializable
data class UpdateSeekerDesiredPositionsRequest(
    val occupationIds: List<Long>,
)

@Serializable
data class SeekerDashboardDto(
    val profileCompletionPercent: Int,
    val desiredPositionsCount: Int,
    val experienceCount: Int,
    val recommendationsPreview: List<JobRecommendationDto>,
    val testsComplete: Boolean,
)

@Serializable
data class SeekerPositionsOverviewDto(
    val occupationIds: List<Long>,
    val occupations: List<OccupationDto>,
    val recommendations: List<JobRecommendationDto>,
    val interests: SeekerInterestsResponseDto,
    val testsComplete: Boolean,
)
