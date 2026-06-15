package jobs.procrush.llm

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmResponseParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun extractFirstJsonObject_ignoresTrailingExtraBrace() {
        val payload = """{"title":"t","n":1}"""
        val raw = "$payload\n}\n"
        val extracted = LlmResponseParser.extractFirstJsonObject(raw)
        assertEquals(payload, extracted)
        json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(extracted)
    }

    @Test
    fun extractJson_stripsMarkdownAndTrailingBrace() {
        val inner = """{"profile":"ok"}"""
        val raw = "```json\n$inner\n```\n}"
        val extracted = LlmResponseParser.extractJson(raw)
        assertEquals(inner, extracted)
    }
}
