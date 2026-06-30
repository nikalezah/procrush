package jobs.procrush.observability

object CorrelationIds {
    const val REQUEST_ID = "requestId"
    const val TRACE_ID = "traceId"
    const val SPAN_ID = "spanId"
    const val USER_ID = "userId"
    const val SEEKER_ID = "seekerId"
    const val EVENT_ID = "eventId"
    const val MESSAGE_ID = "messageId"

    const val HEADER_REQUEST_ID = "X-Request-Id"
    const val HEADER_TRACE_PARENT = "traceparent"
    const val HEADER_TRACE_STATE = "tracestate"
}
