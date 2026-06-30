package jobs.procrush.llm

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import jobs.procrush.bootstrap.config.LlmConfig
import kotlinx.serialization.Serializable

class OllamaLlmClient(
    private val config: LlmConfig,
    httpClient: HttpClient? = null,
) : LlmClient {
    private val httpClient = httpClient ?: LlmFactory.createHttpClient(config)

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val httpResponse =
            httpClient.post("${config.apiRoot}/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequest(
                        model = config.model!!,
                        messages =
                            listOf(
                                OllamaMessage(role = "system", content = systemPrompt),
                                OllamaMessage(role = "user", content = userPrompt),
                            ),
                        stream = false,
                        format = "json",
                    ),
                )
            }
        val rawBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            error("Ollama HTTP ${httpResponse.status.value}: ${rawBody.take(500)}")
        }
        val response = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<OllamaChatResponse>(rawBody)
        return response.message?.content ?: error("Empty Ollama response")
    }
}

@Serializable
private data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean,
    val format: String,
)

@Serializable
private data class OllamaMessage(val role: String, val content: String)

@Serializable
private data class OllamaChatResponse(val message: OllamaMessage? = null)
