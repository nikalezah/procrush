package jobs.procrush.llm

import jobs.procrush.fixtures.StubData
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class StubLlmClient : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        delay(2_000)
        return json.encodeToString(StubData.personalityLlmOutput())
    }
}
