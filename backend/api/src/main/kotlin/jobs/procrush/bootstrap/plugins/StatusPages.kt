package jobs.procrush.bootstrap.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.observability.MdcContext
import jobs.procrush.shared.CodedException
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.shared.RegistrationConflictException
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.SurveyAlreadyCompletedException
import jobs.procrush.shared.toResponseBody
import org.slf4j.LoggerFactory

private val statusPagesLogger = LoggerFactory.getLogger("jobs.procrush.StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<CodedException> { call, cause ->
            statusPagesLogger.warn(
                "requestId={} coded error={} status={}",
                MdcContext.currentRequestId(),
                cause.errorCode.name,
                cause.errorCode.httpStatus,
            )
            call.respond(
                HttpStatusCode.fromValue(cause.errorCode.httpStatus),
                cause.toResponseBody(),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            statusPagesLogger.warn("requestId={} bad request", MdcContext.currentRequestId(), cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorCode.INVALID_REQUEST.toResponseBody(),
            )
        }
        exception<SurveyAlreadyCompletedException> { call, cause ->
            statusPagesLogger.warn("requestId={} survey already completed", MdcContext.currentRequestId())
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<GenerationInProgressException> { call, cause ->
            statusPagesLogger.warn("requestId={} generation in progress", MdcContext.currentRequestId())
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<RegistrationConflictException> { call, cause ->
            statusPagesLogger.warn("requestId={} registration conflict", MdcContext.currentRequestId())
            call.respond(HttpStatusCode.Conflict, cause.toResponseBody())
        }
        exception<ResourceNotFoundException> { call, cause ->
            statusPagesLogger.warn("requestId={} not found", MdcContext.currentRequestId())
            call.respond(HttpStatusCode.NotFound, cause.toResponseBody())
        }
        exception<IllegalStateException> { call, cause ->
            statusPagesLogger.warn("requestId={} illegal state", MdcContext.currentRequestId(), cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorCode.UNKNOWN_ERROR.toResponseBody(),
            )
        }
        exception<Throwable> { call, cause ->
            statusPagesLogger.error("requestId={} unhandled error", MdcContext.currentRequestId(), cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorCode.UNKNOWN_ERROR.toResponseBody(),
            )
        }
    }
}
