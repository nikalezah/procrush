package jobs.procrush.bootstrap.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import jobs.procrush.bootstrap.config.AppConfig

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
