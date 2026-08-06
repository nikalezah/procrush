package jobs.procrush.personality.observability

import jobs.procrush.bootstrap.config.LogFormat
import jobs.procrush.shared.CorrelationIds
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Instant

class Logger private constructor(
    private val name: String,
) {
    fun info(
        message: String,
        throwable: Throwable? = null,
    ) = log("INFO", message, throwable)

    fun warn(
        message: String,
        throwable: Throwable? = null,
    ) = log("WARN", message, throwable)

    fun error(
        message: String,
        throwable: Throwable? = null,
    ) = log("ERROR", message, throwable)

    fun debug(
        message: String,
        throwable: Throwable? = null,
    ) = log("DEBUG", message, throwable)

    fun info(
        format: String,
        vararg args: Any?,
    ) = info(formatMessage(format, args))

    fun error(
        format: String,
        vararg args: Any?,
    ) {
        val throwable = args.lastOrNull() as? Throwable
        val messageArgs = if (throwable != null) args.dropLast(1).toTypedArray() else args
        error(formatMessage(format, messageArgs), throwable)
    }

    fun debug(
        format: String,
        vararg args: Any?,
    ) {
        val throwable = args.lastOrNull() as? Throwable
        val messageArgs = if (throwable != null) args.dropLast(1).toTypedArray() else args
        debug(formatMessage(format, messageArgs), throwable)
    }

    private fun log(
        level: String,
        message: String,
        throwable: Throwable?,
    ) {
        val config = WorkerObservability.config
        val line =
            when (config.logFormat) {
                LogFormat.JSON -> formatJson(level, message, throwable, config)
                LogFormat.TEXT -> formatText(level, message, throwable)
            }
        synchronized(stdoutLock) {
            println(line)
            if (config.logFormat == LogFormat.TEXT && throwable != null) {
                println(throwable.stackTraceToString())
            }
        }
    }

    private fun formatText(
        level: String,
        message: String,
        throwable: Throwable?,
    ): String {
        val timestamp = formatTextTimestamp(Clock.System.now())
        val requestId = Correlation.currentRequestId().orEmpty()
        val suffix = if (throwable != null) "" else ""
        return "$timestamp [${Thread.currentThread().name}] $level $name [$requestId] - $message$suffix"
    }

    private fun formatJson(
        level: String,
        message: String,
        throwable: Throwable?,
        config: WorkerLogConfig,
    ): String {
        val fields = linkedMapOf<String, Any?>(
            "@timestamp" to Clock.System.now().toString(),
            "level" to level,
            "logger_name" to name,
            "message" to message,
            "service" to config.serviceName,
            "environment" to config.environment,
            "thread_name" to Thread.currentThread().name,
        )
        val correlation = Correlation.snapshot()
        listOf(
            CorrelationIds.REQUEST_ID,
            CorrelationIds.MESSAGE_ID,
            CorrelationIds.SEEKER_ID,
            CorrelationIds.USER_ID,
            CorrelationIds.EVENT_ID,
            CorrelationIds.TRACE_ID,
            CorrelationIds.SPAN_ID,
        ).forEach { key ->
            correlation[key]?.let { fields[key] = it }
        }
        throwable?.let {
            fields["stack_trace"] = it.stackTraceToString()
        }
        return fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${escapeJson(key)}\":${jsonValue(value)}"
        }
    }

    companion object {
        private val stdoutLock = Any()

        fun get(kClass: KClass<*>): Logger =
            Logger(kClass.qualifiedName ?: kClass.simpleName!!)

        private fun formatTextTimestamp(instant: Instant): String {
            val local = instant.toLocalDateTime(TimeZone.UTC)
            val millis = (instant.toEpochMilliseconds() % 1000).let { if (it < 0) it + 1000 else it }
            return buildString {
                append(local.year.toString().padStart(4, '0'))
                append('-')
                append(local.monthNumber.toString().padStart(2, '0'))
                append('-')
                append(local.dayOfMonth.toString().padStart(2, '0'))
                append(' ')
                append(local.hour.toString().padStart(2, '0'))
                append(':')
                append(local.minute.toString().padStart(2, '0'))
                append(':')
                append(local.second.toString().padStart(2, '0'))
                append('.')
                append(millis.toString().padStart(3, '0'))
            }
        }

        private fun formatMessage(
            format: String,
            args: Array<out Any?>,
        ): String {
            if (args.isEmpty()) return format
            val parts = format.split("{}")
            if (parts.size == 1) return format
            val builder = StringBuilder()
            for (index in parts.indices) {
                builder.append(parts[index])
                if (index < args.size) {
                    builder.append(args[index])
                }
            }
            return builder.toString()
        }

        private fun jsonValue(value: Any?): String =
            when (value) {
                null -> "null"
                is Number, is Boolean -> value.toString()
                else -> "\"${escapeJson(value.toString())}\""
            }

        private fun escapeJson(value: String): String =
            buildString(value.length + 8) {
                value.forEach { ch ->
                    when (ch) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(ch)
                    }
                }
            }
    }
}
