package jobs.procrush.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SucceedThrough(
    val point0: String,
    val point1: String,
    val point2: String,
) {
    fun asList(): List<String> = listOf(point0, point1, point2)
}

@Serializable
data class PersonalityTraitDetails(
    val description: String,
    @SerialName("good_day") val goodDay: String? = null,
    @SerialName("bad_day") val badDay: String? = null,
    @SerialName("succeed_through")
    @Serializable(with = SucceedThroughSerializer::class)
    val succeedThrough: SucceedThrough? = null,
) {
    fun validateSucceedThrough(path: String) {
        require(succeedThrough != null) {
            "$path.details.succeed_through обязательно"
        }
        succeedThrough.asList().forEachIndexed { index, item ->
            require(item.isNotBlank()) { "$path.details.succeed_through[$index] не может быть пустым" }
        }
    }
}

@Serializable
data class PersonalityTrait(
    val label: String,
    @SerialName("scale_position") val scalePosition: Double,
    @SerialName("left_pole") val leftPole: String,
    @SerialName("right_pole") val rightPole: String,
    val details: PersonalityTraitDetails? = null,
)

@Serializable
data class ConnectionsTraits(
    val trait0: PersonalityTrait,
    val trait1: PersonalityTrait,
    val trait2: PersonalityTrait,
    val trait3: PersonalityTrait,
) {
    fun asList(): List<PersonalityTrait> = listOf(trait0, trait1, trait2, trait3)
}

@Serializable
data class CreativityTraits(
    val trait0: PersonalityTrait,
    val trait1: PersonalityTrait,
    val trait2: PersonalityTrait,
) {
    fun asList(): List<PersonalityTrait> = listOf(trait0, trait1, trait2)
}

@Serializable
data class DriveTraits(
    val trait0: PersonalityTrait,
    val trait1: PersonalityTrait,
    val trait2: PersonalityTrait,
    val trait3: PersonalityTrait,
) {
    fun asList(): List<PersonalityTrait> = listOf(trait0, trait1, trait2, trait3)
}

@Serializable
data class ThinkingTraits(
    val trait0: PersonalityTrait,
) {
    fun asList(): List<PersonalityTrait> = listOf(trait0)
}

@Serializable
data class ConnectionsCategory(
    val description: String,
    @SerialName("top_strength_index") val topStrengthIndex: Int,
    @Serializable(with = ConnectionsTraitsSerializer::class)
    val traits: ConnectionsTraits,
) {
    fun validateStructure() {
        require(description.isNotBlank()) { "connections.description обязательно" }
        require(topStrengthIndex in 0..3) {
            "connections.top_strength_index должен быть от 0 до 3, получено $topStrengthIndex"
        }
    }
}

@Serializable
data class CreativityCategory(
    val description: String,
    @SerialName("top_strength_index") val topStrengthIndex: Int,
    @Serializable(with = CreativityTraitsSerializer::class)
    val traits: CreativityTraits,
) {
    fun validateStructure() {
        require(description.isNotBlank()) { "creativity.description обязательно" }
        require(topStrengthIndex in 0..2) {
            "creativity.top_strength_index должен быть от 0 до 2, получено $topStrengthIndex"
        }
    }
}

@Serializable
data class DriveCategory(
    val description: String,
    @SerialName("top_strength_index") val topStrengthIndex: Int,
    @Serializable(with = DriveTraitsSerializer::class)
    val traits: DriveTraits,
) {
    fun validateStructure() {
        require(description.isNotBlank()) { "drive.description обязательно" }
        require(topStrengthIndex in 0..3) {
            "drive.top_strength_index должен быть от 0 до 3, получено $topStrengthIndex"
        }
    }
}

@Serializable
data class ThinkingCategory(
    val description: String,
    @SerialName("top_strength_index") val topStrengthIndex: Int,
    @Serializable(with = ThinkingTraitsSerializer::class)
    val traits: ThinkingTraits,
) {
    fun validateStructure() {
        require(description.isNotBlank()) { "thinking.description обязательно" }
        require(topStrengthIndex == 0) {
            "thinking.top_strength_index должен быть 0, получено $topStrengthIndex"
        }
    }
}

@Serializable
data class PersonalityItem(
    val title: String,
    val description: String,
) {
    fun validateItem(path: String) {
        require(title.isNotBlank()) { "$path.title обязательно" }
        require(description.isNotBlank()) { "$path.description обязательно" }
    }
}

@Serializable
data class EnergySourcesItems(
    val item0: PersonalityItem,
    val item1: PersonalityItem,
    val item2: PersonalityItem,
) {
    fun asList(): List<PersonalityItem> = listOf(item0, item1, item2)
}

@Serializable
data class StopFactorsItems(
    val item0: PersonalityItem,
    val item1: PersonalityItem,
) {
    fun asList(): List<PersonalityItem> = listOf(item0, item1)
}

@Serializable
data class EnergySourcesSection(
    val title: String,
    @Serializable(with = EnergySourcesItemsSerializer::class)
    val items: EnergySourcesItems,
) {
    fun validateStructure() {
        require(title == PersonalitySectionRules.ENERGY_SOURCES_TITLE) {
            "energy_sources.title должен быть «${PersonalitySectionRules.ENERGY_SOURCES_TITLE}»"
        }
        items.asList().forEachIndexed { index, item ->
            item.validateItem("energy_sources.items[$index]")
        }
    }

    fun toSectionDto(): PersonalitySectionDto =
        PersonalitySectionDto(
            title = title,
            items = items.asList().map { it.toDto() },
        )
}

@Serializable
data class StopFactorsSection(
    val title: String,
    @Serializable(with = StopFactorsItemsSerializer::class)
    val items: StopFactorsItems,
) {
    fun validateStructure() {
        require(title == PersonalitySectionRules.STOP_FACTORS_TITLE) {
            "stop_factors.title должен быть «${PersonalitySectionRules.STOP_FACTORS_TITLE}»"
        }
        items.asList().forEachIndexed { index, item ->
            item.validateItem("stop_factors.items[$index]")
        }
    }

    fun toSectionDto(): PersonalitySectionDto =
        PersonalitySectionDto(
            title = title,
            items = items.asList().map { it.toDto() },
        )
}

fun PersonalityItem.toDto(): PersonalityItemDto = PersonalityItemDto(title = title, description = description)
