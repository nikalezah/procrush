package jobs.procrush.models

import kotlinx.serialization.Serializable

@Serializable
data class OccupationDto(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val isLeaf: Boolean,
)

@Serializable
data class SkillDto(
    val id: Long,
    val name: String,
)

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
)

@Serializable
data class CreateJobProfileRequest(
    val occupationId: Long,
    val description: String? = null,
    val isActive: Boolean = true,
    val skillIds: List<Long> = emptyList(),
)

@Serializable
data class UpdateJobProfileRequest(
    val occupationId: Long,
    val description: String? = null,
    val isActive: Boolean = true,
    val skillIds: List<Long> = emptyList(),
)

@Serializable
enum class PersonalityProfileStatus {
    NOT_READY,
    PROCESSING,
    READY,
    FAILED,
}

object PersonalityTraitDetailsRules {
    const val SUCCEED_THROUGH_SIZE = 3
}

object PersonalitySectionRules {
    const val ENERGY_SOURCES_COUNT = 3
    const val STOP_FACTORS_COUNT = 2
    const val ENERGY_SOURCES_TITLE = "Источники энергии"
    const val STOP_FACTORS_TITLE = "Стоп-факторы"
    const val ITEM_DESCRIPTION_MIN_WORDS = 15
    const val ITEM_DESCRIPTION_MAX_WORDS = 25
}

fun personalityItemWordCount(text: String): Int =
    text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size

@Serializable
data class PersonalityItemDto(
    val title: String,
    val description: String,
) {
    fun validateItem(path: String) {
        require(title.isNotBlank()) { "$path.title обязательно" }
        require(description.isNotBlank()) { "$path.description обязательно" }
        // Временно отключено: ограничение 15–25 слов в description
        // val words = personalityItemWordCount(description)
        // require(words in PersonalitySectionRules.ITEM_DESCRIPTION_MIN_WORDS..PersonalitySectionRules.ITEM_DESCRIPTION_MAX_WORDS) {
        //     "$path.description должно содержать от ${PersonalitySectionRules.ITEM_DESCRIPTION_MIN_WORDS} " +
        //         "до ${PersonalitySectionRules.ITEM_DESCRIPTION_MAX_WORDS} слов, получено $words"
        // }
    }
}

@Serializable
data class PersonalitySectionDto(
    val title: String,
    val items: List<PersonalityItemDto>,
)

@Serializable
data class SeekerPersonalProfileRecord(
    val seekerId: Long,
    val title: String?,
    val description: String?,
    val profile: String?,
    val autonomy: String?,
    val thinkingStyle: String?,
    val burnoutRisk: String?,
    val connections: ConnectionsCategory?,
    val creativity: CreativityCategory?,
    val drive: DriveCategory?,
    val thinking: ThinkingCategory?,
    val axisDominance: Double?,
    val axisInfluence: Double?,
    val axisStability: Double?,
    val axisIntegrity: Double?,
    val axisAutonomy: Double?,
    val axisPace: Double?,
    val burnoutRiskOverload: Double?,
    val burnoutRiskConflicts: Double?,
    val burnoutRiskDemotivation: Double?,
    val burnoutRiskStress: Double?,
    val energySources: EnergySourcesSection?,
    val stopFactors: StopFactorsSection?,
    val generationStatus: PersonalityProfileStatus,
    val generationError: String?,
)

@Serializable
data class SeekerPersonalProfileLlmOutput(
    val title: String,
    val description: String,
    val profile: String,
    val autonomy: String? = null,
    @kotlinx.serialization.SerialName("thinking_style") val thinkingStyle: String? = null,
    @kotlinx.serialization.SerialName("burnout_risk") val burnoutRisk: String? = null,
    val connections: ConnectionsCategory,
    val creativity: CreativityCategory,
    val drive: DriveCategory,
    val thinking: ThinkingCategory,
    @kotlinx.serialization.SerialName("axis_dominance") val axisDominance: Double,
    @kotlinx.serialization.SerialName("axis_influence") val axisInfluence: Double,
    @kotlinx.serialization.SerialName("axis_stability") val axisStability: Double,
    @kotlinx.serialization.SerialName("axis_integrity") val axisIntegrity: Double,
    @kotlinx.serialization.SerialName("axis_autonomy") val axisAutonomy: Double,
    @kotlinx.serialization.SerialName("axis_pace") val axisPace: Double,
    @kotlinx.serialization.SerialName("burnout_risk_overload") val burnoutRiskOverload: Double? = null,
    @kotlinx.serialization.SerialName("burnout_risk_conflicts") val burnoutRiskConflicts: Double? = null,
    @kotlinx.serialization.SerialName("burnout_risk_demotivation") val burnoutRiskDemotivation: Double? = null,
    @kotlinx.serialization.SerialName("burnout_risk_stress") val burnoutRiskStress: Double? = null,
    @kotlinx.serialization.SerialName("energy_sources") val energySources: EnergySourcesSection,
    @kotlinx.serialization.SerialName("stop_factors") val stopFactors: StopFactorsSection,
)

@Serializable
data class SucceedThroughDto(
    val point0: String,
    val point1: String,
    val point2: String,
)

@Serializable
data class PersonalityTraitDetailsDto(
    val description: String,
    val goodDay: String,
    val badDay: String,
    val succeedThrough: SucceedThroughDto,
)

@Serializable
data class PersonalityTraitDto(
    val key: String,
    val label: String,
    val scalePosition: Double,
    val leftPole: String,
    val rightPole: String,
    val details: PersonalityTraitDetailsDto,
    val isTopStrength: Boolean = false,
)

@Serializable
data class PersonalityCategoryDto(
    val key: String,
    val description: String,
    @kotlinx.serialization.SerialName("top_strength_index") val topStrengthIndex: Int,
    val traits: List<PersonalityTraitDto>,
)

@Serializable
data class PersonalityPreviewDto(
    val status: PersonalityProfileStatus,
    val generationError: String? = null,
    val testsCompleted: Int,
    val testsTotal: Int,
    val title: String? = null,
    val description: String? = null,
    val profile: String? = null,
    val autonomy: String? = null,
    val thinkingStyle: String? = null,
    val burnoutRisk: String? = null,
    val axisDominance: Double? = null,
    val axisInfluence: Double? = null,
    val axisStability: Double? = null,
    val axisIntegrity: Double? = null,
    val axisAutonomy: Double? = null,
    val axisPace: Double? = null,
    val categories: List<PersonalityCategoryDto>? = null,
    val energySources: PersonalitySectionDto? = null,
    val stopFactors: PersonalitySectionDto? = null,
)

@Serializable
data class JobRecommendationDto(
    val id: Long,
    val companyName: String,
    val positionName: String,
    val description: String,
    val matchScore: Double,
    val matchScoreDisplay: Int,
    val testsCompleted: Int,
    val isScoreReduced: Boolean,
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
    val testsCompleted: Int,
    val isScoreReduced: Boolean,
)

@Serializable
data class SeekerDashboardDto(
    val profileCompletionPercent: Int,
    val desiredPositionsCount: Int,
    val experienceCount: Int,
    val recommendationsPreview: List<JobRecommendationDto>,
)

@Serializable
data class EmployerDashboardDto(
    val companyName: String,
    val jobProfilesCount: Int,
    val activeJobProfilesCount: Int,
    val totalCandidatesStub: Int,
)
