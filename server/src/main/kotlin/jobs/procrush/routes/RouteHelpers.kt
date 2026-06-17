package jobs.procrush.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.requireLongParam(name: String = "id"): Long? {
    val id = parameters[name]?.toLongOrNull()
    if (id == null) {
        respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
    }
    return id
}
