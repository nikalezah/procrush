package jobs.procrush.llm

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual fun createLlmHttpClientEngine(): HttpClientEngineFactory<*> = CIO
