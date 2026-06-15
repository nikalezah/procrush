package jobs.procrush.domain

import jobs.procrush.models.ConnectionsCategory
import jobs.procrush.models.CreativityCategory
import jobs.procrush.models.DriveCategory
import jobs.procrush.models.PersonalityCategoryDto
import jobs.procrush.models.PersonalityPreviewDto
import jobs.procrush.models.PersonalityProfileStatus
import jobs.procrush.models.PersonalityTrait
import jobs.procrush.models.PersonalityTraitDetailsDto
import jobs.procrush.models.PersonalityTraitDto
import jobs.procrush.models.SeekerPersonalProfileLlmOutput
import jobs.procrush.models.SeekerPersonalProfileRecord
import jobs.procrush.models.SucceedThroughDto
import jobs.procrush.models.ThinkingCategory

object PersonalityProfileMapper {
    fun toPreview(
        record: SeekerPersonalProfileRecord,
        testsCompleted: Int,
        testsTotal: Int,
    ): PersonalityPreviewDto {
        val categories = buildCategories(record)
        return PersonalityPreviewDto(
            status = PersonalityProfileStatus.READY,
            testsCompleted = testsCompleted,
            testsTotal = testsTotal,
            title = record.title,
            description = record.description,
            profile = record.profile,
            autonomy = record.autonomy,
            thinkingStyle = record.thinkingStyle,
            burnoutRisk = record.burnoutRisk,
            axisDominance = record.axisDominance,
            axisInfluence = record.axisInfluence,
            axisStability = record.axisStability,
            axisIntegrity = record.axisIntegrity,
            axisAutonomy = record.axisAutonomy,
            axisPace = record.axisPace,
            categories = categories,
            energySources = record.energySources?.toSectionDto(),
            stopFactors = record.stopFactors?.toSectionDto(),
        )
    }

    fun fromLlmOutput(seekerId: Long, output: SeekerPersonalProfileLlmOutput): SeekerPersonalProfileRecord =
        SeekerPersonalProfileRecord(
            seekerId = seekerId,
            title = output.title,
            description = output.description,
            profile = output.profile,
            autonomy = output.autonomy,
            thinkingStyle = output.thinkingStyle,
            burnoutRisk = output.burnoutRisk,
            connections = output.connections,
            creativity = output.creativity,
            drive = output.drive,
            thinking = output.thinking,
            axisDominance = output.axisDominance,
            axisInfluence = output.axisInfluence,
            axisStability = output.axisStability,
            axisIntegrity = output.axisIntegrity,
            axisAutonomy = output.axisAutonomy,
            axisPace = output.axisPace,
            burnoutRiskOverload = output.burnoutRiskOverload,
            burnoutRiskConflicts = output.burnoutRiskConflicts,
            burnoutRiskDemotivation = output.burnoutRiskDemotivation,
            burnoutRiskStress = output.burnoutRiskStress,
            energySources = output.energySources,
            stopFactors = output.stopFactors,
            generationStatus = PersonalityProfileStatus.READY,
            generationError = null,
        )

    private fun buildCategories(record: SeekerPersonalProfileRecord): List<PersonalityCategoryDto> {
        val entries =
            listOf(
                "connections" to record.connections,
                "creativity" to record.creativity,
                "drive" to record.drive,
                "thinking" to record.thinking,
            )
        return entries.mapNotNull { (key, category) ->
            category?.let { categoryToDto(key, category) }
        }
    }

    private fun categoryToDto(
        key: String,
        category: Any,
    ): PersonalityCategoryDto =
        when (category) {
            is ConnectionsCategory ->
                PersonalityCategoryDto(
                    key = key,
                    description = category.description,
                    topStrengthIndex = category.topStrengthIndex,
                    traits = traitsToDtos(key, category.topStrengthIndex, category.traits.asList()),
                )
            is CreativityCategory ->
                PersonalityCategoryDto(
                    key = key,
                    description = category.description,
                    topStrengthIndex = category.topStrengthIndex,
                    traits = traitsToDtos(key, category.topStrengthIndex, category.traits.asList()),
                )
            is DriveCategory ->
                PersonalityCategoryDto(
                    key = key,
                    description = category.description,
                    topStrengthIndex = category.topStrengthIndex,
                    traits = traitsToDtos(key, category.topStrengthIndex, category.traits.asList()),
                )
            is ThinkingCategory ->
                PersonalityCategoryDto(
                    key = key,
                    description = category.description,
                    topStrengthIndex = category.topStrengthIndex,
                    traits = traitsToDtos(key, category.topStrengthIndex, category.traits.asList()),
                )
            else -> error("Неизвестная категория: $key")
        }

    private fun traitsToDtos(
        categoryKey: String,
        topStrengthIndex: Int,
        traits: List<PersonalityTrait>,
    ): List<PersonalityTraitDto> =
        traits.mapIndexed { index, trait ->
            val details = trait.details ?: error("$categoryKey.traits[$index].details обязательно")
            val succeed = details.succeedThrough ?: error("$categoryKey.traits[$index].details.succeed_through обязательно")
            PersonalityTraitDto(
                key = "${categoryKey}_$index",
                label = trait.label,
                scalePosition = trait.scalePosition,
                leftPole = trait.leftPole,
                rightPole = trait.rightPole,
                details =
                    PersonalityTraitDetailsDto(
                        description = details.description,
                        goodDay = details.goodDay.orEmpty(),
                        badDay = details.badDay.orEmpty(),
                        succeedThrough =
                            SucceedThroughDto(
                                point0 = succeed.point0,
                                point1 = succeed.point1,
                                point2 = succeed.point2,
                            ),
                    ),
                isTopStrength = index == topStrengthIndex,
            )
        }
}
