package jobs.procrush.personality.dto

import kotlinx.serialization.Serializable

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
    }
}

@Serializable
data class PersonalitySectionDto(
    val title: String,
    val items: List<PersonalityItemDto>,
)

@Serializable
data class SuperpowerAndTalentLlmItem(
    val name: String,
    @kotlinx.serialization.SerialName("is_pronounced") val isPronounced: Boolean,
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
    @kotlinx.serialization.SerialName("superpowers_and_talents") val superpowersAndTalents: List<SuperpowerAndTalentLlmItem>,
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
    val superpowersAndTalents: List<jobs.procrush.shared.dto.SuperpowerAndTalentDto>? = null,
)
