package jobs.procrush.llm

import io.ktor.client.engine.HttpClientEngineFactory

/** Platform HTTP engine: CIO on JVM, Curl on Native (TLS-capable). */
internal expect fun createLlmHttpClientEngine(): HttpClientEngineFactory<*>
