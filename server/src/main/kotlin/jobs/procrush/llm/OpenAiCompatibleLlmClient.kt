package jobs.procrush.llm

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import jobs.procrush.config.LlmConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenAiCompatibleLlmClient(
    private val config: LlmConfig,
    httpClient: HttpClient? = null,
) : LlmClient {
    private val httpClient = httpClient ?: LlmFactory.createHttpClient(config)

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val httpResponse =
            httpClient.post(config.chatCompletionsUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                config.httpReferer?.let { header("HTTP-Referer", it) }
                config.appTitle?.let { header("X-Title", it) }
                setBody(
                    OpenAiChatRequest(
                        model = config.model!!,
                        messages =
                            listOf(
                                OpenAiMessage(role = "system", content = systemPrompt),
                                OpenAiMessage(role = "user", content = userPrompt),
                            ),
                        responseFormat = OpenAiResponseFormat(type = "json_object"),
                    ),
                )
            }

        val rawBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            error("LLM HTTP ${httpResponse.status.value}: ${extractErrorMessage(rawBody)}")
        }

        val response = json.decodeFromString<OpenAiChatResponse>(rawBody)
        response.error?.message?.let { error("LLM error: $it") }

        return extractAssistantText(response.choices?.firstOrNull()?.message, rawBody)
    }

    private fun extractAssistantText(message: OpenAiMessage?, rawBody: String): String {
        val text =
            message?.content?.takeIf { it.isNotBlank() }
                ?: message?.reasoning?.takeIf { it.isNotBlank() }
        return text ?: error("Пустой ответ от LLM: ${rawBody.take(500)}")
    }

    private fun extractErrorMessage(rawBody: String): String {
        return runCatching {
            json.decodeFromString<OpenAiChatResponse>(rawBody).error?.message
        }.getOrNull() ?: rawBody.take(500)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @SerialName("response_format") val responseFormat: OpenAiResponseFormat? = null,
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    val reasoning: String? = null,
)

@Serializable
private data class OpenAiResponseFormat(val type: String)

@Serializable
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiApiError? = null,
)

@Serializable
private data class OpenAiApiError(val message: String? = null)

@Serializable
private data class OpenAiChoice(val message: OpenAiMessage? = null)
