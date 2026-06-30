package jobs.procrush.observability

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.header
import org.slf4j.MDC
import java.util.UUID

fun Application.configureRequestContext() {
    intercept(ApplicationCallPipeline.Call) {
        val requestId =
            call.request.header(CorrelationIds.HEADER_REQUEST_ID)?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()
        call.response.header(CorrelationIds.HEADER_REQUEST_ID, requestId)
        val previous = MDC.getCopyOfContextMap()
        MdcContext.put(CorrelationIds.REQUEST_ID, requestId)
        try {
            proceed()
        } finally {
            if (previous == null) {
                MDC.clear()
            } else {
                MDC.setContextMap(previous)
            }
        }
    }
}
