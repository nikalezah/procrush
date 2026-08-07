package jobs.procrush.personality.amqp

/**
 * Parses `amqp://user:pass@host:port/vhost` without `java.net.URI`.
 * Preserves prior URI semantics for host/port/path/userInfo.
 */
data class AmqpUrl(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    /** Decoded virtual host (default `/`). */
    val virtualHost: String,
    val raw: String,
) {
    companion object {
        fun parse(amqpUrl: String): AmqpUrl {
            val withoutScheme =
                when {
                    amqpUrl.startsWith("amqp://") -> amqpUrl.removePrefix("amqp://")
                    amqpUrl.startsWith("amqps://") -> amqpUrl.removePrefix("amqps://")
                    else -> amqpUrl
                }
            val (userInfo, hostAndPath) =
                if ('@' in withoutScheme) {
                    val at = withoutScheme.indexOf('@')
                    withoutScheme.substring(0, at) to withoutScheme.substring(at + 1)
                } else {
                    null to withoutScheme
                }
            val user = userInfo?.substringBefore(':')?.takeIf { it.isNotEmpty() } ?: "guest"
            val password =
                if (userInfo != null && ':' in userInfo) {
                    percentDecode(userInfo.substringAfter(':'))
                } else {
                    "guest"
                }

            val slash = hostAndPath.indexOf('/')
            val hostPort = if (slash >= 0) hostAndPath.substring(0, slash) else hostAndPath
            val vhostPath = if (slash >= 0) hostAndPath.substring(slash) else ""

            val host =
                when {
                    hostPort.startsWith("[") && "]" in hostPort ->
                        hostPort.substring(1, hostPort.indexOf(']'))
                    else -> hostPort.substringBefore(':').ifBlank { "localhost" }
                }
            val port =
                when {
                    hostPort.startsWith("[") && "]:" in hostPort ->
                        hostPort.substringAfter("]:").toIntOrNull() ?: 5672
                    !hostPort.startsWith("[") && ':' in hostPort ->
                        hostPort.substringAfter(':').toIntOrNull() ?: 5672
                    else -> 5672
                }

            val virtualHost = decodeVirtualHost(vhostPath)
            return AmqpUrl(
                host = host,
                port = port,
                user = percentDecode(user),
                password = password,
                virtualHost = virtualHost,
                raw = amqpUrl,
            )
        }

        private fun decodeVirtualHost(vhostPath: String): String {
            val raw = vhostPath.trimStart('/')
            if (raw.isEmpty()) return "/"
            return percentDecode(raw).ifEmpty { "/" }
        }

        private fun percentDecode(value: String): String {
            if ('%' !in value && '+' !in value) return value
            val bytes = ArrayList<Byte>(value.length)
            var i = 0
            while (i < value.length) {
                val c = value[i]
                when {
                    c == '%' && i + 2 < value.length -> {
                        val hex = value.substring(i + 1, i + 3)
                        val decoded = hex.toIntOrNull(16)
                        if (decoded != null) {
                            bytes.add(decoded.toByte())
                            i += 3
                        } else {
                            bytes.add(c.code.toByte())
                            i += 1
                        }
                    }
                    c == '+' -> {
                        bytes.add(' '.code.toByte())
                        i += 1
                    }
                    else -> {
                        bytes.add(c.code.toByte())
                        i += 1
                    }
                }
            }
            return bytes.toByteArray().decodeToString()
        }
    }
}
