package jobs.procrush.llm

import kotlinx.coroutines.delay

class StubLlmClient : LlmClient {
    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        delay(2_000)
        return STUB_PERSONALITY_JSON
    }

    private companion object {
        private val STUB_PERSONALITY_JSON =
            """
            {
              "title": "Стратегический аналитик",
              "description": "Stub profile for local development.",
              "profile": "Сбалансированный профиль для тестов.",
              "autonomy": 0.6,
              "thinking_style": 0.7,
              "burnout_risk": 0.4,
              "axis_dominance": 0.5,
              "axis_influence": 0.5,
              "axis_stability": 0.5,
              "axis_integrity": 0.5,
              "axis_autonomy": 0.6,
              "axis_pace": 0.5,
              "burnout_risk_overload": 0.4,
              "burnout_risk_conflicts": 0.4,
              "burnout_risk_demotivation": 0.4,
              "burnout_risk_stress": 0.4,
              "superpowers_and_talents": [
                {"name": "Стратегический лидер", "is_pronounced": true}
              ]
            }
            """.trimIndent()
    }
}
