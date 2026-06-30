package jobs.procrush.bootstrap.config

enum class LlmProvider {
    OPENAI_COMPAT,
    OLLAMA,
}

data class LlmConfig(
    val provider: LlmProvider,
    val apiKey: String?,
    val model: String?,
    val baseUrl: String,
    val httpReferer: String?,
    val appTitle: String?,
    val requestTimeoutSeconds: Long,
) {
    /** API root, e.g. `https://openrouter.ai/api/v1` or `http://localhost:11434`. */
    val apiRoot: String
        get() = LlmUrl.normalizeBaseUrl(baseUrl)

    /** POST target for OpenAI-compatible chat APIs (OpenRouter, Groq, etc.). */
    val chatCompletionsUrl: String
        get() = "$apiRoot/chat/completions"

    fun validateForGeneration() {
        when (provider) {
            LlmProvider.OLLAMA ->
                require(!model.isNullOrBlank()) { "LLM is not configured: set LLM_MODEL for Ollama" }
            LlmProvider.OPENAI_COMPAT -> {
                require(!apiKey.isNullOrBlank()) {
                    "LLM is not configured: set LLM_API_KEY"
                }
                require(!model.isNullOrBlank()) {
                    "LLM is not configured: set LLM_MODEL (e.g. google/gemini-2.0-flash-001 on OpenRouter)"
                }
            }
        }
    }

    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>, frontendUrl: String): LlmConfig {
            val baseUrl = LlmUrl.normalizeBaseUrl(Env.env("LLM_BASE_URL", "https://openrouter.ai/api/v1", dotEnv))
            return LlmConfig(
                provider = resolveProvider(Env.resolve("LLM_PROVIDER", dotEnv), baseUrl),
                apiKey = Env.resolve("LLM_API_KEY", dotEnv),
                model = Env.resolve("LLM_MODEL", dotEnv),
                baseUrl = baseUrl,
                httpReferer = Env.resolve("LLM_HTTP_REFERER", dotEnv) ?: frontendUrl,
                appTitle = Env.resolve("LLM_APP_TITLE", dotEnv) ?: "ProCrush",
                requestTimeoutSeconds =
                    Env.env("LLM_REQUEST_TIMEOUT_SECONDS", "180", dotEnv).toLongOrNull()?.coerceAtLeast(30) ?: 180,
            )
        }

        private fun resolveProvider(explicit: String?, baseUrl: String): LlmProvider {
            explicit?.trim()?.lowercase()?.let { raw ->
                return when (raw) {
                    "ollama" -> LlmProvider.OLLAMA
                    "openai", "openai_compat", "openrouter" -> LlmProvider.OPENAI_COMPAT
                    else -> error("Unknown LLM_PROVIDER: $raw (expected ollama, openai)")
                }
            }
            return if (baseUrl.contains("11434") || baseUrl.endsWith("/ollama", ignoreCase = true)) {
                LlmProvider.OLLAMA
            } else {
                LlmProvider.OPENAI_COMPAT
            }
        }
    }
}

object LlmUrl {
    /** Accepts both `https://host/api/v1` and `.../api/v1/chat/completions`. */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim().trimEnd('/')
        while (url.endsWith("/chat/completions", ignoreCase = true)) {
            url = url.removeSuffix("/chat/completions").trimEnd('/')
        }
        return url
    }
}
