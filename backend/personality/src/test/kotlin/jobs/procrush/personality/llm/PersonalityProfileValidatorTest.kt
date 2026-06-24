package jobs.procrush.personality.llm

import jobs.procrush.fixtures.PersonalityStub
import jobs.procrush.personality.dto.PersonalityDbJson
import jobs.procrush.personality.dto.SuperpowerAndTalentLlmItem
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PersonalityProfileValidatorTest {
    private val validator = PersonalityProfileValidator()
    private val catalogNames =
        setOf(
            "Стратегический лидер",
            "Лидерство в неопределенности",
            "Системный анализ",
            "Принятие решений",
            "Мотивация команды",
            "Работа с данными",
            "Коммуникация и влияние",
            "Адаптивность и обучаемость",
        )

    @Test
    fun validateAndParse_acceptsValidSuperpowers() {
        val json = PersonalityDbJson.encodeToString(PersonalityStub.personalityLlmOutput())
        validator.validateAndParse(json, catalogNames)
    }

    @Test
    fun validateAndParse_rejectsTooFewSuperpowers() {
        val output =
            PersonalityStub.personalityLlmOutput().copy(
                superpowersAndTalents =
                    listOf(
                        SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = true),
                        SuperpowerAndTalentLlmItem(name = "Принятие решений", isPronounced = true),
                        SuperpowerAndTalentLlmItem(name = "Работа с данными", isPronounced = false),
                    ),
            )
        val json = PersonalityDbJson.encodeToString(output)
        assertFailsWith<IllegalArgumentException> {
            validator.validateAndParse(json, catalogNames)
        }
    }

    @Test
    fun validateAndParse_rejectsUnknownSuperpowerName() {
        val output =
            PersonalityStub.personalityLlmOutput().copy(
                superpowersAndTalents =
                    listOf(
                        SuperpowerAndTalentLlmItem(name = "Неизвестная суперсила", isPronounced = true),
                        SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = true),
                        SuperpowerAndTalentLlmItem(name = "Принятие решений", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Работа с данными", isPronounced = false),
                    ),
            )
        val json = PersonalityDbJson.encodeToString(output)
        assertFailsWith<IllegalArgumentException> {
            validator.validateAndParse(json, catalogNames)
        }
    }

    @Test
    fun validateAndParse_rejectsDuplicateSuperpowerNames() {
        val output =
            PersonalityStub.personalityLlmOutput().copy(
                superpowersAndTalents =
                    listOf(
                        SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = true),
                        SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Принятие решений", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Работа с данными", isPronounced = false),
                    ),
            )
        val json = PersonalityDbJson.encodeToString(output)
        assertFailsWith<IllegalArgumentException> {
            validator.validateAndParse(json, catalogNames)
        }
    }

    @Test
    fun validateAndParse_rejectsWhenNoPronouncedSuperpower() {
        val output =
            PersonalityStub.personalityLlmOutput().copy(
                superpowersAndTalents =
                    listOf(
                        SuperpowerAndTalentLlmItem(name = "Стратегический лидер", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Принятие решений", isPronounced = false),
                        SuperpowerAndTalentLlmItem(name = "Работа с данными", isPronounced = false),
                    ),
            )
        val json = PersonalityDbJson.encodeToString(output)
        assertFailsWith<IllegalArgumentException> {
            validator.validateAndParse(json, catalogNames)
        }
    }
}
