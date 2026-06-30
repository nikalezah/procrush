package jobs.procrush.survey.scoring

import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.raise
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true }

object SurveyAnswerValidator {
    fun validate(surveyCode: String, questionsJson: String, answers: JsonElement) {
        val questions = json.parseToJsonElement(questionsJson).jsonObject
        val type = questions["type"]?.jsonPrimitive?.content ?: ErrorCode.SURVEY_UNKNOWN_TYPE.raise()
        val answersObj = answers as? JsonObject ?: ErrorCode.SURVEY_ANSWERS_MUST_BE_OBJECT.raise()
        when (type) {
            "open_questions" -> validateOpenQuestions(questions, answersObj)
            "multi_select" -> validateMultiSelect(questions, answersObj)
            "allocate_points" -> validateAllocatePoints(questions, answersObj)
            "scale_0_4" -> validateScale(questions, answersObj)
            "binary_choice" -> validateBinaryChoice(questions, answersObj)
            "belbin_matrix" -> validateBelbin(questions, answersObj)
            else -> ErrorCode.SURVEY_UNSUPPORTED_TYPE.raise(mapOf("type" to type))
        }
    }

    private fun validateOpenQuestions(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: ErrorCode.SURVEY_NO_QUESTIONS.raise()
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val text = answers[id]?.jsonPrimitive?.content?.trim().orEmpty()
            if (text.isBlank()) {
                ErrorCode.SURVEY_ANSWER_REQUIRED.raise(mapOf("questionId" to id))
            }
        }
    }

    private fun validateMultiSelect(questions: JsonObject, answers: JsonObject) {
        val key = questions["answerKey"]?.jsonPrimitive?.content ?: ErrorCode.INVALID_REQUEST.raise()
        val min = questions["minSelections"]?.jsonPrimitive?.int ?: ErrorCode.INVALID_REQUEST.raise()
        val max = questions["maxSelections"]?.jsonPrimitive?.int ?: min
        val selected = answers[key]?.jsonArray ?: ErrorCode.SURVEY_ANSWERS_NOT_FOUND.raise()
        if (selected.size !in min..max) {
            ErrorCode.SURVEY_SELECTION_COUNT_INVALID.raise(mapOf("min" to min.toString(), "max" to max.toString()))
        }
    }

    private fun validateAllocatePoints(questions: JsonObject, answers: JsonObject) {
        val total = questions["totalPoints"]?.jsonPrimitive?.int ?: 10
        val maxPer = questions["maxPerOption"]?.jsonPrimitive?.int ?: 5
        val items = questions["questions"]?.jsonArray ?: ErrorCode.SURVEY_NO_QUESTIONS.raise()
        items.forEach { q ->
            val qId = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val block = answers["q$qId"]?.jsonObject ?: answers[qId]?.jsonObject
                ?: ErrorCode.SURVEY_ANSWER_REQUIRED.raise(mapOf("questionId" to qId))
            var sum = 0
            block.forEach { (_, v) ->
                val pts = v.jsonPrimitive.intOrNull ?: ErrorCode.SURVEY_POINTS_INVALID.raise(mapOf("maxPer" to maxPer.toString()))
                if (pts !in 0..maxPer) {
                    ErrorCode.SURVEY_POINTS_INVALID.raise(mapOf("maxPer" to maxPer.toString()))
                }
                sum += pts
            }
            if (sum != total) {
                ErrorCode.SURVEY_POINTS_SUM_INVALID.raise(mapOf("questionId" to qId, "total" to total.toString()))
            }
        }
    }

    private fun validateScale(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: ErrorCode.SURVEY_NO_QUESTIONS.raise()
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val value = answers[id]?.jsonPrimitive?.intOrNull
                ?: ErrorCode.SURVEY_ANSWER_REQUIRED.raise(mapOf("questionId" to id))
            if (value !in 0..4) {
                ErrorCode.SURVEY_SCALE_VALUE_INVALID.raise(mapOf("questionId" to id))
            }
        }
    }

    private fun validateBinaryChoice(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: ErrorCode.SURVEY_NO_QUESTIONS.raise()
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val choice = answers[id]?.jsonPrimitive?.intOrNull
                ?: ErrorCode.SURVEY_ANSWER_REQUIRED.raise(mapOf("questionId" to id))
            if (choice !in 1..2) {
                ErrorCode.SURVEY_BINARY_CHOICE_INVALID.raise(mapOf("questionId" to id))
            }
        }
    }

    private fun validateBelbin(questions: JsonObject, answers: JsonObject) {
        val total = questions["totalPoints"]?.jsonPrimitive?.int ?: 10
        val maxPer = questions["maxPerOption"]?.jsonPrimitive?.int ?: 5
        val items = questions["questions"]?.jsonArray ?: ErrorCode.SURVEY_NO_QUESTIONS.raise()
        items.forEach { q ->
            val qId = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val block = answers["section_$qId"]?.jsonObject
                ?: ErrorCode.SURVEY_ANSWER_REQUIRED.raise(mapOf("questionId" to qId))
            var sum = 0
            block.forEach { (_, v) ->
                val pts = v.jsonPrimitive.intOrNull ?: ErrorCode.SURVEY_POINTS_INVALID.raise(mapOf("maxPer" to maxPer.toString()))
                if (pts !in 0..maxPer) {
                    ErrorCode.SURVEY_POINTS_INVALID.raise(mapOf("maxPer" to maxPer.toString()))
                }
                sum += pts
            }
            if (sum != total) {
                ErrorCode.SURVEY_POINTS_SUM_INVALID.raise(mapOf("questionId" to qId, "total" to total.toString()))
            }
        }
    }
}

object SurveyScoringService {
    fun calculate(
        surveyCode: String,
        scoringLogic: String,
        keysDataJson: String,
        questionsJson: String,
        answersJson: String,
    ): String {
        val keys = json.parseToJsonElement(keysDataJson).jsonObject
        val questions = json.parseToJsonElement(questionsJson).jsonObject
        val answers = json.parseToJsonElement(answersJson).jsonObject
        return when (scoringLogic) {
            "open_text" -> scoreOpenText(questions, answers)
            "matrix" -> scoreMatrix(surveyCode, keys, questions, answers)
            "direct_sum" -> scoreDirectSum(keys, answers)
            "formula" -> scoreFormula(surveyCode, keys, answers)
            else -> ErrorCode.SURVEY_UNSUPPORTED_SCORING_LOGIC.raise(mapOf("logic" to scoringLogic))
        }
    }

    private fun scoreOpenText(questions: JsonObject, answers: JsonObject): String {
        val items = questions["questions"]?.jsonArray ?: JsonArray(emptyList())
        val responses =
            buildJsonArray {
                items.forEach { q ->
                    val obj = q.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content.orEmpty()
                    add(
                        buildJsonObject {
                            put("n", id.toIntOrNull() ?: 0)
                            put("question", obj["text"]?.jsonPrimitive?.content.orEmpty())
                            put("answer", answers[id]?.jsonPrimitive?.content.orEmpty())
                        },
                    )
                }
            }
        return buildJsonObject { put("responses", responses) }.toString()
    }

    private fun scoreMatrix(
        surveyCode: String,
        keys: JsonObject,
        questions: JsonObject,
        answers: JsonObject,
    ): String =
        when {
            surveyCode.endsWith("ETYA") || surveyCode.endsWith("NEYA") ->
                scoreQualities(keys, answers, questions["answerKey"]?.jsonPrimitive?.content.orEmpty())
            surveyCode.endsWith("12F4") -> score12F4(keys, answers)
            surveyCode.endsWith("BLBN") -> scoreBlbn(keys, answers)
            else -> error("Matrix scoring not configured for $surveyCode")
        }

    private fun scoreQualities(keys: JsonObject, answers: JsonObject, answerKey: String): String {
        val weights = keys["qualities_weights"]?.jsonObject ?: error("qualities_weights is missing")
        val selected = answers[answerKey]?.jsonArray ?: error("Answers not found")
        val poles = listOf("A3", "A1", "B3", "B1", "C3", "C1", "D3", "D1")
        val totals = poles.associateWith { 0 }.toMutableMap()
        val items =
            buildJsonArray {
                selected.forEach { el ->
                    val id = el.jsonPrimitive.content
                    val w = weights[id]?.jsonObject
                    val item =
                        buildJsonObject {
                            put("id", id.toIntOrNull() ?: 0)
                            put("label", w?.get("label")?.jsonPrimitive?.content.orEmpty())
                            poles.forEach { p ->
                                put(p, w?.get(p)?.jsonPrimitive?.intOrNull ?: 0)
                                totals[p] = totals.getValue(p) + (w?.get(p)?.jsonPrimitive?.intOrNull ?: 0)
                            }
                        }
                    add(item)
                }
            }
        return buildJsonObject {
            put("items", items)
            put("axis_totals", buildJsonObject { poles.forEach { put(it, totals.getValue(it)) } })
        }.toString()
    }

    private fun score12F4(keys: JsonObject, answers: JsonObject): String {
        val mapping = keys["blocks_mapping"]?.jsonObject ?: error("blocks_mapping is missing")
        val totals = mutableMapOf("Q" to 0, "W" to 0, "E" to 0, "R" to 0)
        val blocks =
            buildJsonArray {
                mapping.forEach { (blockId, opts) ->
                    val blockAnswers = answers["q$blockId"]?.jsonObject ?: answers[blockId]?.jsonObject
                    val blockObj = buildJsonObject {
                        put("block", blockId.toIntOrNull() ?: 0)
                        val optionsObj = buildJsonObject {}
                        val factors = mutableMapOf("Q" to 0, "W" to 0, "E" to 0, "R" to 0)
                        opts.jsonObject.forEach { (optId, factorEl) ->
                            val pts = blockAnswers?.get(optId)?.jsonPrimitive?.intOrNull ?: 0
                            val factor = factorEl.jsonPrimitive.content
                            factors[factor] = factors.getValue(factor) + pts
                            totals[factor] = totals.getValue(factor) + pts
                        }
                        put("options", blockAnswers ?: buildJsonObject {})
                        factors.forEach { (f, v) -> put(f, v) }
                    }
                    add(blockObj)
                }
            }
        return buildJsonObject {
            put("blocks", blocks)
            put("totals", buildJsonObject { totals.forEach { (k, v) -> put(k, v) } })
        }.toString()
    }

    private fun scoreBlbn(keys: JsonObject, answers: JsonObject): String {
        val roleMapping = keys["role_mapping"]?.jsonObject ?: error("role_mapping is missing")
        val roles = keys["roles"]?.jsonArray?.map { it.jsonPrimitive.content } ?: BELBIN_DEFAULT_ROLES
        val roleTotals = roles.associateWith { 0 }.toMutableMap()
        val questionsArr =
            buildJsonArray {
                roleMapping.forEach { (qId, opts) ->
                    val block = answers["section_$qId"]?.jsonObject ?: buildJsonObject {}
                    val rolesBlock = buildJsonObject {
                        roles.forEach { role ->
                            val sum = opts.jsonObject.entries.sumOf { (optId, roleEl) ->
                                if (roleEl.jsonPrimitive.content == role) {
                                    block[optId]?.jsonPrimitive?.intOrNull ?: 0
                                } else {
                                    0
                                }
                            }
                            roleTotals[role] = (roleTotals[role] ?: 0) + sum
                            put(role, sum)
                        }
                    }
                    add(
                        buildJsonObject {
                            put("n", qId.toIntOrNull() ?: 0)
                            put("options", block)
                            put("roles", rolesBlock)
                        },
                    )
                }
            }
        return buildJsonObject {
            put("questions", questionsArr)
            put("role_totals", buildJsonObject { roleTotals.forEach { (k, v) -> put(k, v) } })
        }.toString()
    }

    private fun scoreDirectSum(keys: JsonObject, answers: JsonObject): String {
        val qMap = keys["questions_map"]?.jsonObject ?: error("questions_map is missing")
        val items =
            buildJsonArray {
                qMap.forEach { (id, meta) ->
                    val raw = answers[id]?.jsonPrimitive?.intOrNull ?: 0
                    val direction = meta.jsonObject["direction"]?.jsonPrimitive?.intOrNull ?: 1
                    val pole = meta.jsonObject["pole"]?.jsonPrimitive?.content.orEmpty()
                    val value = raw * direction
                    add(
                        buildJsonObject {
                            put("n", id.toIntOrNull() ?: 0)
                            put("pole", pole)
                            put("value", value)
                        },
                    )
                }
            }
        return buildJsonObject { put("items", items) }.toString()
    }

    private fun scoreFormula(surveyCode: String, keys: JsonObject, answers: JsonObject): String =
        when {
            surveyCode.endsWith("1IN2") -> score1In2(keys, answers)
            surveyCode.endsWith("GNFL") -> scoreGnfl(keys, answers)
            surveyCode.endsWith("RDFL") -> scoreRdfl(keys, answers)
            else -> error("Formula scoring not configured for $surveyCode")
        }

    private fun score1In2(keys: JsonObject, answers: JsonObject): String {
        val codeMap = keys["answers_code_map"]?.jsonObject ?: error("answers_code_map is missing")
        val dilemmas =
            buildJsonArray {
                codeMap.forEach { (id, opts) ->
                    val choice = answers[id]?.jsonPrimitive?.content.orEmpty()
                    val code = opts.jsonObject[choice]?.jsonPrimitive?.content.orEmpty()
                    add(
                        buildJsonObject {
                            put("n", id.toIntOrNull() ?: 0)
                            put("choice", choice.toIntOrNull() ?: 0)
                            put("code", code)
                        },
                    )
                }
            }
        return buildJsonObject { put("dilemmas", dilemmas) }.toString()
    }

    private fun scoreGnfl(keys: JsonObject, answers: JsonObject): String {
        val codes = keys["factor_codes"]?.jsonObject ?: error("factor_codes is missing")
        val selected = answers["positive_factors"]?.jsonArray ?: error("positive_factors is missing")
        val factors =
            buildJsonArray {
                selected.forEachIndexed { index, el ->
                    val id = el.jsonPrimitive.content
                    add(
                        buildJsonObject {
                            put("n", index + 1)
                            put("id", id.toIntOrNull() ?: 0)
                            put("code", codes[id]?.jsonPrimitive?.content.orEmpty())
                        },
                    )
                }
            }
        return buildJsonObject { put("factors", factors) }.toString()
    }

    private fun scoreRdfl(keys: JsonObject, answers: JsonObject): String {
        val meta = keys["factor_meta"]?.jsonObject ?: error("factor_meta is missing")
        val selected = answers["annoying_factors"]?.jsonArray ?: error("annoying_factors is missing")
        val factors =
            buildJsonArray {
                selected.forEachIndexed { index, el ->
                    val id = el.jsonPrimitive.content
                    val m = meta[id]?.jsonObject
                    add(
                        buildJsonObject {
                            put("n", index + 1)
                            put("id", id.toIntOrNull() ?: 0)
                            put("code", m?.get("code")?.jsonPrimitive?.content.orEmpty())
                            put("risk_dr", m?.get("risk_dr")?.jsonPrimitive?.content.orEmpty())
                            put("level", m?.get("level")?.jsonPrimitive?.intOrNull ?: 0)
                            put("vector", m?.get("vector")?.jsonPrimitive?.content.orEmpty())
                            put("agency", m?.get("agency")?.jsonPrimitive?.content.orEmpty())
                        },
                    )
                }
            }
        return buildJsonObject { put("factors", factors) }.toString()
    }

    private val BELBIN_DEFAULT_ROLES =
        listOf("BR_CO", "BR_PL", "BR_CF", "BR_SH", "BR_IM", "BR_ME", "BR_RI", "BR_TW")
}
