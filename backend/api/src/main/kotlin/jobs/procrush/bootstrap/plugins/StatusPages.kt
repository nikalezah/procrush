package jobs.procrush.bootstrap.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.CodedException
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.shared.RegistrationConflictException
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.SurveyAlreadyCompletedException
import jobs.procrush.shared.toResponseBody

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<CodedException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.errorCode.httpStatus),
                cause.toResponseBody(),
            )
        }
        exception<IllegalArgumentException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorCode.INVALID_REQUEST.toResponseBody(),
            )
        }
        exception<SurveyAlreadyCompletedException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<GenerationInProgressException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<RegistrationConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<ResourceNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.toResponseBody())
        }
        exception<IllegalStateException> { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorCode.NOT_FOUND.toResponseBody(),
            )
        }
    }
}
