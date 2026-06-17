package jobs.procrush.domain

import jobs.procrush.survey.SurveyLlmContextDto
import kotlinx.serialization.json.Json

class PersonalityPromptBuilder {
    private val json = Json { prettyPrint = true }

    fun build(context: SurveyLlmContextDto): Pair<String, String> {
        val systemPrompt =
            """
            Ты — опытный психолог-интерпретатор результатов личностных опросов.
            На основе предоставленных ответов, расчётов и глоссария сформируй целостный личностный профиль соискателя.

            ВАЖНО — формат ответа (соблюдай буквально):
            - Верни ТОЛЬКО один JSON-объект: от первой «{» до парной закрывающей «}», без текста до и после.
            - Не используй markdown (никаких ``` или ```json).
            - Не добавляй лишнюю «}» или «{» в конце — ровно одна закрывающая скобка корневого объекта.
            - Перед отправкой мысленно проверь: валидный JSON, все строки в двойных кавычках, внутри строк экранируй «"» как \\" и переносы как \\n.
            - Числа — без кавычек; массивы traits/items — только JSON-массивы фиксированной длины, не объекты с произвольными ключами.

            JSON должен соответствовать следующей структуре (все поля обязательны, кроме помеченных optional):

            {
              "title": "string — краткий тип личности",
              "description": "string — общее описание",
              "profile": "string — развёрнутый профиль",
              "autonomy": "string — описание автономности (optional)",
              "thinking_style": "string — стиль мышления (optional)",
              "burnout_risk": "string — общий риск выгорания (optional)",
              "connections": { "description": "string", "top_strength_index": 0, "traits": [ { "label": "string", "scale_position": 0.0-1.0, "left_pole": "string", "right_pole": "string", "details": { "description": "string", "good_day": "string", "bad_day": "string", "succeed_through": ["string", "string", "string"] } }, ... ровно 4 объекта ] },
              "creativity": { "description": "string", "top_strength_index": 0, "traits": [ ... ровно 3 объекта ] },
              "drive": { "description": "string", "top_strength_index": 0, "traits": [ ... ровно 4 объекта ] },
              "thinking": { "description": "string", "top_strength_index": 0, "traits": [ ... ровно 1 объект ] },
              "axis_dominance": 0.0-1.0,
              "axis_influence": 0.0-1.0,
              "axis_stability": 0.0-1.0,
              "axis_integrity": 0.0-1.0,
              "axis_autonomy": 0.0-1.0,
              "axis_pace": 0.0-1.0,
              "burnout_risk_overload": 0.0-1.0 (optional),
              "burnout_risk_conflicts": 0.0-1.0 (optional),
              "burnout_risk_demotivation": 0.0-1.0 (optional),
              "burnout_risk_stress": 0.0-1.0 (optional),
              "energy_sources": { "title": "Источники энергии", "items": [
                { "title": "string", "description": "string — ровно 20–30 слов" },
                { "title": "string", "description": "string — ровно 20–30 слов" },
                { "title": "string", "description": "string — ровно 20–30 слов" }
              ] },
              "stop_factors": { "title": "Стоп-факторы", "items": [
                { "title": "string", "description": "string — ровно 20–30 слов" },
                { "title": "string", "description": "string — ровно 20–30 слов" }
              ] }
            }

            Используй термины из глоссария. Опирайся на calculated_results каждого опроса.
            Пиши на русском языке. Числовые значения осей — нормализованные от 0 до 1.

            Правила для категорий connections, creativity, drive, thinking:
            - description категории — вводный абзац по шаблону «Ваш раздел … показывает …» (что измеряет категория).
            - traits — массив фиксированной длины: connections — ровно 4, creativity — ровно 3, drive — ровно 4, thinking — ровно 1.
            - top_strength_index — целое число 0..N-1: индекс ЕДИНСТВЕННОЙ «главной силы» в массиве traits этой категории (самая выраженная черта).
            - label черты — заголовок в духе «Вы немного более {правый полюс}, чем {левый}» или «Вы {доминирующий полюс}» при сильном смещении.
            - left_pole и right_pole — короткие русские названия полюсов шкалы.
            - scale_position: 0 = левый полюс, 1 = правый полюс.
            - details обязательны для каждой черты: description, good_day, bad_day, succeed_through (ровно 3 коротких пункта — чем вы добиваетесь успеха).

            Правила для energy_sources и stop_factors:
            - energy_sources.title — строго «Источники энергии»; items — ровно 3 объекта (ни больше, ни меньше).
            - stop_factors.title — строго «Стоп-факторы» (не «факторы торможения» и не другие формулировки); items — ровно 2 объекта.
            - У каждого элемента: title — короткий заголовок; description — связный текст ровно из 15–25 слов (считай слова по пробелам).

            Финальная проверка перед ответом: один корневой объект, без хвоста после «}», без комментариев // и без trailing comma.
            """.trimIndent()

        val userPrompt =
            """
            Данные для интерпретации:

            ${json.encodeToString(context)}
            """.trimIndent()

        return systemPrompt to userPrompt
    }
}
