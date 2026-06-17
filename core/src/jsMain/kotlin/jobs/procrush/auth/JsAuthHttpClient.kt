package jobs.procrush.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
