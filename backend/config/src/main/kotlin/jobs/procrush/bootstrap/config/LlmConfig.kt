package jobs.procrush.bootstrap.config

enum class LlmProvider {
    STUB,
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
    val stubMode: Boolean
        get() = provider == LlmProvider.STUB

    /** API root, e.g. `https://openrouter.ai/api/v1` or `http://localhost:11434`. */
    val apiRoot: String
        get() = LlmUrl.normalizeBaseUrl(baseUrl)

    /** POST target for OpenAI-compatible chat APIs (OpenRouter, Groq, etc.). */
    val chatCompletionsUrl: String
        get() = "$apiRoot/chat/completions"

    fun validateForGeneration() {
        if (stubMode) return
        when (provider) {
            LlmProvider.OLLAMA ->
                require(!model.isNullOrBlank()) { "LLM не настроен: укажите LLM_MODEL для Ollama" }
            LlmProvider.OPENAI_COMPAT -> {
                require(!apiKey.isNullOrBlank()) {
                    "LLM не настроен: укажите LLM_API_KEY или включите LLM_USE_STUB=true для локальной разработки"
                }
                require(!model.isNullOrBlank()) {
                    "LLM не настроен: укажите LLM_MODEL (например google/gemini-2.0-flash-001 на OpenRouter)"
                }
            }
            LlmProvider.STUB -> Unit
        }
    }

    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>, frontendUrl: String): LlmConfig {
            val stubMode = Env.env("LLM_USE_STUB", "false", dotEnv).equals("true", ignoreCase = true)
            val baseUrl = LlmUrl.normalizeBaseUrl(Env.env("LLM_BASE_URL", "https://openrouter.ai/api/v1", dotEnv))
            val provider =
                when {
                    stubMode -> LlmProvider.STUB
                    else -> resolveProvider(Env.resolve("LLM_PROVIDER", dotEnv), baseUrl)
                }
            return LlmConfig(
                provider = provider,
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
                    "stub" -> LlmProvider.STUB
                    "ollama" -> LlmProvider.OLLAMA
                    "openai", "openai_compat", "openrouter" -> LlmProvider.OPENAI_COMPAT
                    else -> error("Неизвестный LLM_PROVIDER: $raw (ожидается stub, ollama, openai)")
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
