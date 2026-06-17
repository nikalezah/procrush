package jobs.procrush.config

data class LlmConfig(
    val apiKey: String?,
    val model: String?,
    val baseUrl: String,
    val stubMode: Boolean,
    val httpReferer: String?,
    val appTitle: String?,
    val requestTimeoutSeconds: Long,
) {
    val useStub: Boolean
        get() = stubMode

    val isOllama: Boolean
        get() = apiRoot.contains("11434") || apiRoot.endsWith("/ollama", ignoreCase = true)

    /** API root, e.g. `https://openrouter.ai/api/v1` or `http://localhost:11434`. */
    val apiRoot: String
        get() = normalizedBaseUrl

    /** POST target for OpenAI-compatible chat APIs (OpenRouter, Groq, etc.). */
    val chatCompletionsUrl: String
        get() = "$apiRoot/chat/completions"

    private val normalizedBaseUrl: String
        get() = normalizeBaseUrl(baseUrl)

    fun validateForGeneration() {
        if (useStub) return
        if (isOllama) {
            require(!model.isNullOrBlank()) { "LLM не настроен: укажите LLM_MODEL для Ollama" }
            return
        }
        require(!apiKey.isNullOrBlank()) {
            "LLM не настроен: укажите LLM_API_KEY или включите LLM_USE_STUB=true для локальной разработки"
        }
        require(!model.isNullOrBlank()) {
            "LLM не настроен: укажите LLM_MODEL (например google/gemini-2.0-flash-001 на OpenRouter)"
        }
    }

    companion object {
        fun fromEnvironment(dotEnv: Map<String, String>, frontendUrl: String): LlmConfig {
            val stubMode = env("LLM_USE_STUB", "false", dotEnv).equals("true", ignoreCase = true)
            return LlmConfig(
                apiKey = resolve("LLM_API_KEY", dotEnv),
                model = resolve("LLM_MODEL", dotEnv),
                baseUrl = normalizeBaseUrl(env("LLM_BASE_URL", "https://openrouter.ai/api/v1", dotEnv)),
                stubMode = stubMode,
                httpReferer = resolve("LLM_HTTP_REFERER", dotEnv) ?: frontendUrl,
                appTitle = resolve("LLM_APP_TITLE", dotEnv) ?: "ProCrush",
                requestTimeoutSeconds =
                    env("LLM_REQUEST_TIMEOUT_SECONDS", "180", dotEnv).toLongOrNull()?.coerceAtLeast(30) ?: 180,
            )
        }

        private fun env(name: String, default: String, dotEnv: Map<String, String>): String =
            resolve(name, dotEnv) ?: default

        private fun resolve(name: String, dotEnv: Map<String, String>): String? =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: dotEnv[name]?.takeIf { it.isNotBlank() }

        /** Accepts both `https://host/api/v1` and `.../api/v1/chat/completions`. */
        fun normalizeBaseUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            while (url.endsWith("/chat/completions", ignoreCase = true)) {
                url = url.removeSuffix("/chat/completions").trimEnd('/')
            }
            return url
        }
    }
}
