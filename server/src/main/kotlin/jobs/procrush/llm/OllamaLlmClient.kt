package jobs.procrush.llm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import jobs.procrush.config.LlmConfig
import kotlinx.serialization.Serializable

class OllamaLlmClient(
    private val config: LlmConfig,
    httpClient: HttpClient? = null,
) : LlmClient {
    private val httpClient = httpClient ?: LlmHttpClientFactory.create(config)

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val response: OllamaChatResponse =
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
            }.body()
        return response.message?.content ?: error("Пустой ответ от Ollama")
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
