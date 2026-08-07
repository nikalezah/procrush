package jobs.procrush.llm

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.curl.Curl

internal actual fun createLlmHttpClientEngine(): HttpClientEngineFactory<*> = Curl
