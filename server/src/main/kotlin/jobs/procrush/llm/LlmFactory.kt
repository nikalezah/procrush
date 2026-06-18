package jobs.procrush.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.bootstrap.config.LlmProvider
import kotlinx.serialization.json.Json

object LlmFactory {
    fun createClient(config: LlmConfig): LlmClient {
        config.validateForGeneration()
        return when (config.provider) {
            LlmProvider.STUB -> StubLlmClient()
            LlmProvider.OLLAMA -> OllamaLlmClient(config)
            LlmProvider.OPENAI_COMPAT -> OpenAiCompatibleLlmClient(config)
        }
    }

    fun createHttpClient(config: LlmConfig): HttpClient {
        val timeoutMs = config.requestTimeoutSeconds * 1_000
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = timeoutMs
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = timeoutMs
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
