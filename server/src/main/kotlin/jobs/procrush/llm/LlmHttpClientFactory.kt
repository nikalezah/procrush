package jobs.procrush.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import jobs.procrush.config.LlmConfig
import kotlinx.serialization.json.Json

object LlmHttpClientFactory {
    fun create(config: LlmConfig): HttpClient {
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
