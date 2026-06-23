package jobs.procrush.personality.llm

import jobs.procrush.llm.LlmResponseParser
import jobs.procrush.personality.dto.PersonalityDbJson
import jobs.procrush.personality.dto.PersonalityTrait
import jobs.procrush.personality.dto.SeekerPersonalProfileLlmOutput
import jobs.procrush.personality.dto.SuperpowerAndTalentLlmItem

class PersonalityProfileValidator {
    private val json = PersonalityDbJson

    fun validateAndParse(rawResponse: String, catalogNames: Set<String>): SeekerPersonalProfileLlmOutput {
        val jsonText = LlmResponseParser.extractJson(rawResponse)
        val output =
            try {
                json.decodeFromString<SeekerPersonalProfileLlmOutput>(jsonText)
            } catch (e: Exception) {
                throw IllegalArgumentException("Не удалось разобрать JSON ответа LLM: ${e.message}")
            }

        require(output.title.isNotBlank()) { "Поле title обязательно" }
        require(output.description.isNotBlank()) { "Поле description обязательно" }
        require(output.profile.isNotBlank()) { "Поле profile обязательно" }

        listOf(
            output.axisDominance,
            output.axisInfluence,
            output.axisStability,
            output.axisIntegrity,
            output.axisAutonomy,
            output.axisPace,
        ).forEach { requireScore(it, "axis") }

        output.burnoutRiskOverload?.let { requireScore(it, "burnout_risk_overload") }
        output.burnoutRiskConflicts?.let { requireScore(it, "burnout_risk_conflicts") }
        output.burnoutRiskDemotivation?.let { requireScore(it, "burnout_risk_demotivation") }
        output.burnoutRiskStress?.let { requireScore(it, "burnout_risk_stress") }

        output.connections.validateStructure()
        output.creativity.validateStructure()
        output.drive.validateStructure()
        output.thinking.validateStructure()
        validateTraits("connections", output.connections.traits.asList())
        validateTraits("creativity", output.creativity.traits.asList())
        validateTraits("drive", output.drive.traits.asList())
        validateTraits("thinking", output.thinking.traits.asList())
        output.energySources.validateStructure()
        output.stopFactors.validateStructure()
        validateSuperpowers(output.superpowersAndTalents, catalogNames)

        return output
    }

    private fun validateSuperpowers(items: List<SuperpowerAndTalentLlmItem>, catalogNames: Set<String>) {
        require(items.size in 4..6) {
            "superpowers_and_talents должно содержать от 4 до 6 элементов, получено ${items.size}"
        }
        val names = items.map { it.name.trim() }
        require(names.all { it.isNotBlank() }) { "superpowers_and_talents.name обязательно" }
        require(names.toSet().size == names.size) { "superpowers_and_talents: дубликаты названий запрещены" }
        names.forEach { name ->
            require(name in catalogNames) { "superpowers_and_talents: неизвестное название «$name»" }
        }
        require(items.any { it.isPronounced }) {
            "superpowers_and_talents: минимум один элемент с is_pronounced: true"
        }
    }

    private fun requireScore(value: Double, field: String) {
        require(value in 0.0..1.0) { "$field должно быть в диапазоне 0.0–1.0, получено $value" }
    }

    private fun validateTraits(name: String, traits: List<PersonalityTrait>) {
        traits.forEachIndexed { index, trait ->
            val path = "$name.traits[$index]"
            require(trait.label.isNotBlank()) { "$path.label обязательно" }
            require(trait.leftPole.isNotBlank()) { "$path.left_pole обязательно" }
            require(trait.rightPole.isNotBlank()) { "$path.right_pole обязательно" }
            requireScore(trait.scalePosition, "$path.scale_position")
            val details = trait.details
            require(details != null) { "$path.details обязательно" }
            require(details.description.isNotBlank()) { "$path.details.description обязательно" }
            require(!details.goodDay.isNullOrBlank()) { "$path.details.good_day обязательно" }
            require(!details.badDay.isNullOrBlank()) { "$path.details.bad_day обязательно" }
            details.validateSucceedThrough(path)
        }
    }
}
