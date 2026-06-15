package jobs.procrush.auth

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal fun createAuthHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        if (ApiConfig.apiBaseUrl.isNotBlank()) {
            install(DefaultRequest) {
                url(ApiConfig.apiBaseUrl)
            }
        }
    }

internal suspend fun HttpClient.authGet(path: String) =
    get(path) {
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }

internal suspend fun HttpClient.authPost(path: String, body: Any? = null) =
    post(path) {
        header(HttpHeaders.Accept, ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        if (body != null) {
            setBody(body)
        }
    }
