package jobs.procrush.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PersonalityAxesDto(
    val axisDominance: Double,
    val axisInfluence: Double,
    val axisStability: Double,
    val axisIntegrity: Double,
    val axisAutonomy: Double,
    val axisPace: Double,
) {
    fun validate() {
        listOf(
            axisDominance,
            axisInfluence,
            axisStability,
            axisIntegrity,
            axisAutonomy,
            axisPace,
        ).forEach { value ->
            require(value in 0.0..1.0) { "Значение оси должно быть от 0 до 1" }
        }
    }

    fun asList(): List<Double> =
        listOf(
            axisDominance,
            axisInfluence,
            axisStability,
            axisIntegrity,
            axisAutonomy,
            axisPace,
        )

    companion object {
        val DEFAULT = PersonalityAxesDto(
            axisDominance = 0.5,
            axisInfluence = 0.5,
            axisStability = 0.5,
            axisIntegrity = 0.5,
            axisAutonomy = 0.5,
            axisPace = 0.5,
        )

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String?): PersonalityAxesDto {
            if (raw.isNullOrBlank()) return DEFAULT
            return runCatching { json.decodeFromString<PersonalityAxesDto>(raw) }
                .getOrDefault(DEFAULT)
        }

        fun toJson(axes: PersonalityAxesDto): String = json.encodeToString(axes)

        fun fromSeekerRecord(record: SeekerPersonalProfileRecord): PersonalityAxesDto? {
            val dominance = record.axisDominance ?: return null
            val influence = record.axisInfluence ?: return null
            val stability = record.axisStability ?: return null
            val integrity = record.axisIntegrity ?: return null
            val autonomy = record.axisAutonomy ?: return null
            val pace = record.axisPace ?: return null
            return PersonalityAxesDto(
                axisDominance = dominance,
                axisInfluence = influence,
                axisStability = stability,
                axisIntegrity = integrity,
                axisAutonomy = autonomy,
                axisPace = pace,
            )
        }
    }
}
