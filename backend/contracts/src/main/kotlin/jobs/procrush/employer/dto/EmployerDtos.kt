package jobs.procrush.employer.dto

import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.shared.dto.SkillDto
import kotlinx.serialization.Serializable

@Serializable
data class EmployerProfileDto(
    val id: Long,
    val name: String,
    val description: String?,
    val website: String?,
    val phone: String?,
    val emailContact: String?,
)

@Serializable
data class UpdateEmployerProfileRequest(
    val name: String,
    val description: String? = null,
    val website: String? = null,
    val phone: String? = null,
    val emailContact: String? = null,
)

@Serializable
data class JobProfileDto(
    val id: Long,
    val occupationId: Long,
    val occupationName: String,
    val description: String?,
    val isActive: Boolean,
    val skillIds: List<Long>,
    val skills: List<SkillDto>,
    val personalityAxes: PersonalityAxesDto,
)

@Serializable
data class CreateJobProfileRequest(
    val occupationId: Long,
    val description: String? = null,
    val isActive: Boolean = true,
    val skillIds: List<Long> = emptyList(),
    val personalityAxes: PersonalityAxesDto? = null,
)

@Serializable
data class UpdateJobProfileRequest(
    val occupationId: Long,
    val description: String? = null,
    val isActive: Boolean = true,
    val skillIds: List<Long> = emptyList(),
    val personalityAxes: PersonalityAxesDto? = null,
)

@Serializable
data class EmployerDashboardDto(
    val companyName: String,
    val jobProfilesCount: Int,
    val activeJobProfilesCount: Int,
    val totalMatchedCandidates: Int,
)
