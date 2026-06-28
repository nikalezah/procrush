package jobs.procrush.bootstrap.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.toResponseBody

suspend fun ApplicationCall.requireLongParam(name: String = "id"): Long? {
    val id = parameters[name]?.toLongOrNull()
    if (id == null) {
        respond(HttpStatusCode.BadRequest, ErrorCode.INVALID_ID.toResponseBody())
    }
    return id
}
