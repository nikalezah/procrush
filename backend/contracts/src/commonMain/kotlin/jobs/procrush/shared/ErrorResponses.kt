package jobs.procrush.shared

import jobs.procrush.i18n.ErrorCode

fun ErrorCode.toResponseBody(details: Map<String, String> = emptyMap()): Map<String, Any?> {
    val body = linkedMapOf<String, Any?>(
        "code" to name,
        "message" to formatMessage(details),
    )
    if (details.isNotEmpty()) {
        body["details"] = details
    }
    return body
}

fun CodedException.toResponseBody(): Map<String, Any?> = errorCode.toResponseBody(details)
