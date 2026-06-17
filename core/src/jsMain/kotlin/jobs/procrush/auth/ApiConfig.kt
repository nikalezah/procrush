package jobs.procrush.auth

object ApiConfig {
    /** Empty string uses same origin (webpack dev server proxies /api to Ktor). */
    val apiBaseUrl: String =
        (js("globalThis.PROCRUSH_API_URL") as? String)?.takeIf { it.isNotBlank() }
            ?: ""
}
