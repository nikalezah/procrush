package jobs.procrush.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import jobs.procrush.config.AppConfig

fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        config.webOrigins.forEach { origin ->
            val hostWithPort = origin.removePrefix("http://").removePrefix("https://")
            val scheme = if (origin.startsWith("https")) "https" else "http"
            allowHost(hostWithPort, schemes = listOf(scheme))
        }
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}
