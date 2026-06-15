package jobs.procrush.survey.scoring

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        val type = questions["type"]?.jsonPrimitive?.content ?: error("Неизвестный тип опроса")
        val answersObj = answers as? JsonObject ?: error("Ответы должны быть объектом")
        when (type) {
            "open_questions" -> validateOpenQuestions(questions, answersObj)
            "multi_select" -> validateMultiSelect(questions, answersObj)
            "allocate_points" -> validateAllocatePoints(questions, answersObj)
            "scale_0_4" -> validateScale(questions, answersObj)
            "binary_choice" -> validateBinaryChoice(questions, answersObj)
            "belbin_matrix" -> validateBelbin(questions, answersObj)
            else -> error("Неподдерживаемый тип опроса: $type")
        }
    }

    private fun validateOpenQuestions(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: error("Нет вопросов")
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val text = answers[id]?.jsonPrimitive?.content?.trim().orEmpty()
            require(text.isNotBlank()) { "Ответ на вопрос $id обязателен" }
        }
    }

    private fun validateMultiSelect(questions: JsonObject, answers: JsonObject) {
        val key = questions["answerKey"]?.jsonPrimitive?.content ?: error("answerKey обязателен")
        val min = questions["minSelections"]?.jsonPrimitive?.int ?: error("minSelections обязателен")
        val max = questions["maxSelections"]?.jsonPrimitive?.int ?: min
        val selected = answers[key]?.jsonArray ?: error("Выберите варианты ответа")
        require(selected.size in min..max) { "Нужно выбрать $min${if (max != min) "..$max" else ""} вариантов" }
    }

    private fun validateAllocatePoints(questions: JsonObject, answers: JsonObject) {
        val total = questions["totalPoints"]?.jsonPrimitive?.int ?: 10
        val maxPer = questions["maxPerOption"]?.jsonPrimitive?.int ?: 5
        val items = questions["questions"]?.jsonArray ?: error("Нет вопросов")
        items.forEach { q ->
            val qId = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val block = answers["q$qId"]?.jsonObject ?: answers[qId]?.jsonObject
                ?: error("Заполните распределение для вопроса $qId")
            var sum = 0
            block.forEach { (_, v) ->
                val pts = v.jsonPrimitive.intOrNull ?: error("Некорректные баллы")
                require(pts in 0..maxPer) { "Баллы должны быть от 0 до $maxPer" }
                sum += pts
            }
            require(sum == total) { "Сумма баллов для вопроса $qId должна быть $total" }
        }
    }

    private fun validateScale(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: error("Нет вопросов")
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val value = answers[id]?.jsonPrimitive?.intOrNull ?: error("Ответьте на вопрос $id")
            require(value in 0..4) { "Значение для вопроса $id должно быть 0–4" }
        }
    }

    private fun validateBinaryChoice(questions: JsonObject, answers: JsonObject) {
        val items = questions["questions"]?.jsonArray ?: error("Нет вопросов")
        items.forEach { q ->
            val id = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val choice = answers[id]?.jsonPrimitive?.intOrNull ?: error("Выберите вариант для дилеммы $id")
            require(choice in 1..2) { "Дилемма $id: выберите 1 или 2" }
        }
    }

    private fun validateBelbin(questions: JsonObject, answers: JsonObject) {
        val total = questions["totalPoints"]?.jsonPrimitive?.int ?: 10
        val maxPer = questions["maxPerOption"]?.jsonPrimitive?.int ?: 5
        val items = questions["questions"]?.jsonArray ?: error("Нет вопросов")
        items.forEach { q ->
            val qId = q.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
            val block = answers["section_$qId"]?.jsonObject ?: error("Заполните вопрос $qId")
            var sum = 0
            block.forEach { (_, v) ->
                val pts = v.jsonPrimitive.intOrNull ?: error("Некорректные баллы")
                require(pts in 0..maxPer) { "Баллы должны быть от 0 до $maxPer" }
                sum += pts
            }
            require(sum == total) { "Сумма баллов для вопроса $qId должна быть $total" }
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
            else -> error("Неподдерживаемая scoring_logic: $scoringLogic")
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
            else -> error("Matrix scoring не настроен для $surveyCode")
        }

    private fun scoreQualities(keys: JsonObject, answers: JsonObject, answerKey: String): String {
        val weights = keys["qualities_weights"]?.jsonObject ?: error("qualities_weights отсутствует")
        val selected = answers[answerKey]?.jsonArray ?: error("Ответы не найдены")
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
        val mapping = keys["blocks_mapping"]?.jsonObject ?: error("blocks_mapping отсутствует")
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
        val roleMapping = keys["role_mapping"]?.jsonObject ?: error("role_mapping отсутствует")
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
        val qMap = keys["questions_map"]?.jsonObject ?: error("questions_map отсутствует")
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
            else -> error("Formula scoring не настроен для $surveyCode")
        }

    private fun score1In2(keys: JsonObject, answers: JsonObject): String {
        val codeMap = keys["answers_code_map"]?.jsonObject ?: error("answers_code_map отсутствует")
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
        val codes = keys["factor_codes"]?.jsonObject ?: error("factor_codes отсутствует")
        val selected = answers["positive_factors"]?.jsonArray ?: error("positive_factors отсутствует")
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
        val meta = keys["factor_meta"]?.jsonObject ?: error("factor_meta отсутствует")
        val selected = answers["annoying_factors"]?.jsonArray ?: error("annoying_factors отсутствует")
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
