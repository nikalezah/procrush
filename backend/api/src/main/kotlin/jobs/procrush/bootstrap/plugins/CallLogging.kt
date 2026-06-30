package jobs.procrush.bootstrap.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import jobs.procrush.observability.CorrelationIds
import jobs.procrush.observability.MdcContext
import org.slf4j.event.Level

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            val requestId = MdcContext.currentRequestId() ?: call.response.headers[CorrelationIds.HEADER_REQUEST_ID]
            "$httpMethod $path - $status requestId=$requestId"
        }
    }
}
