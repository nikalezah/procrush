package jobs.procrush.survey.scoring

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurveyScoringServiceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun score5KeyOpenText() {
        val questions =
            """
            {"type":"open_questions","questions":[
              {"id":1,"text":"Карьера мечты — это когда..."},
              {"id":2,"text":"Мой жизненный девиз — это..."}
            ]}
            """.trimIndent()
        val answers =
            """
            {"1":"занимаешься любимым делом","2":"относись к людям так, как хочешь чтобы относились к тебе"}
            """.trimIndent()
        val result =
            SurveyScoringService.calculate(
                surveyCode = "2501-10-5KEY",
                scoringLogic = "open_text",
                keysDataJson = "{}",
                questionsJson = questions,
                answersJson = answers,
            )
        val parsed = json.parseToJsonElement(result).jsonObject
        val responses = parsed["responses"]!!.jsonArray
        assertEquals(2, responses.size)
        assertEquals("занимаешься любимым делом", responses[0].jsonObject["answer"]!!.jsonPrimitive.content)
    }

    @Test
    fun score1In2() {
        val keys =
            """
            {"answers_code_map":{"1":{"1":"S","2":"R"},"2":{"1":"W","2":"Yo"}}}
            """.trimIndent()
        val answers = """{"1":2,"2":2}"""
        val result =
            SurveyScoringService.calculate(
                surveyCode = "2550-10-1IN2",
                scoringLogic = "formula",
                keysDataJson = keys,
                questionsJson = "{}",
                answersJson = answers,
            )
        val dilemmas = json.parseToJsonElement(result).jsonObject["dilemmas"]!!.jsonArray
        assertEquals("R", dilemmas[0].jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals("Yo", dilemmas[1].jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun score12F4Totals() {
        val keys =
            """
            {"blocks_mapping":{"1":{"1":"Q","2":"W","3":"E","4":"R"}}}
            """.trimIndent()
        val answers = """{"q1":{"1":5,"2":2,"3":2,"4":1}}"""
        val result =
            SurveyScoringService.calculate(
                surveyCode = "2530-10-12F4",
                scoringLogic = "matrix",
                keysDataJson = keys,
                questionsJson = "{}",
                answersJson = answers,
            )
        val totals = json.parseToJsonElement(result).jsonObject["totals"]!!.jsonObject
        assertEquals(5, totals["Q"]!!.jsonPrimitive.int)
        assertEquals(2, totals["W"]!!.jsonPrimitive.int)
    }

    @Test
    fun validateMultiSelectCount() {
        val questions =
            """
            {"type":"multi_select","answerKey":"chosen_qualities","minSelections":12,"maxSelections":12,"options":[]}
            """.trimIndent()
        val answers = """{"chosen_qualities":[1,2,3]}"""
        try {
            SurveyAnswerValidator.validate(
                "2520-10-ETYA",
                questions,
                json.parseToJsonElement(answers),
            )
            error("Expected validation failure")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("12"))
        }
    }
}
