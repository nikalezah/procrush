package jobs.procrush.personality.llm

import jobs.procrush.personality.dto.ConnectionsCategory
import jobs.procrush.personality.dto.CreativityCategory
import jobs.procrush.personality.dto.DriveCategory
import jobs.procrush.personality.dto.PersonalityCategoryDto
import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.PersonalityTrait
import jobs.procrush.personality.dto.PersonalityTraitDetailsDto
import jobs.procrush.personality.dto.PersonalityTraitDto
import jobs.procrush.personality.dto.SeekerPersonalProfileRecord
import jobs.procrush.personality.dto.SucceedThroughDto
import jobs.procrush.personality.dto.ThinkingCategory
import jobs.procrush.shared.dto.SuperpowerAndTalentDto

object PersonalityProfilePreviewMapper {
    fun toPreview(
        record: SeekerPersonalProfileRecord,
        testsCompleted: Int,
        testsTotal: Int,
        superpowersAndTalents: List<SuperpowerAndTalentDto> = emptyList(),
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
            superpowersAndTalents = superpowersAndTalents.ifEmpty { null },
        )
    }

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
