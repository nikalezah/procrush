package jobs.procrush.personality.observability

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DlqDepthPoller(
    private val rabbitMqUrl: String,
    private val queueName: String,
    private val intervalSeconds: Long = 30,
) {
    private val logger = Logger.get(DlqDepthPoller::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
    }

    fun start() {
        if (job != null) return
        job =
            scope.launch {
                while (isActive) {
                    runCatching { pollOnce() }
                        .onFailure { error -> logger.debug("RabbitMQ DLQ depth poll failed", error) }
                    delay(intervalSeconds * 1000)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        httpClient.close()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun pollOnce() {
        val managementUri = toManagementQueueUri(rabbitMqUrl, queueName)
        val credentials = parseCredentials(rabbitMqUrl)
        val auth = Base64.encode("${credentials.first}:${credentials.second}".encodeToByteArray())
        val body =
            runBlocking {
                val response =
                    httpClient.get(managementUri) {
                        header(HttpHeaders.Authorization, "Basic $auth")
                    }
                if (!response.status.isSuccess()) return@runBlocking null
                response.bodyAsText()
            } ?: return
        val messagesRegex = """"messages"\s*:\s*(\d+)""".toRegex()
        val depth = messagesRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: return
        Metrics.setRabbitMqQueueDepth(queueName, depth)
    }

    companion object {
        private fun toManagementQueueUri(
            amqpUrl: String,
            queueName: String,
        ): String {
            val parts = parseAmqpUrl(amqpUrl)
            val host = parts.host.ifBlank { "localhost" }
            val port = if (parts.port > 0) parts.port + 10000 else 15672
            val vhost = encodeVhost(parts.vhostPath)
            val encodedQueue = queueName.replace("/", "%2F")
            return "http://$host:$port/api/queues/$vhost/$encodedQueue"
        }

        private fun parseCredentials(amqpUrl: String): Pair<String, String> {
            val parts = parseAmqpUrl(amqpUrl)
            return parts.user to parts.password
        }

        /**
         * AMQP path is `/vhost` or `/%2F` (default). Management API wants the vhost
         * segment encoded once with no leading slash: `%2F` or `custom`.
         */
        private fun encodeVhost(vhostPath: String): String {
            val raw = vhostPath.trimStart('/')
            if (raw.isEmpty()) return "%2F"
            if ('%' in raw) return raw
            return raw.replace("/", "%2F")
        }

        /**
         * Parses `amqp://user:pass@host:port/vhost` without `java.net.URI`.
         * Preserves prior URI semantics for host/port/path/userInfo.
         */
        private fun parseAmqpUrl(amqpUrl: String): AmqpUrlParts {
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
                    userInfo.substringAfter(':')
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
                    else -> hostPort.substringBefore(':')
                }
            val port =
                when {
                    hostPort.startsWith("[") && "]:" in hostPort ->
                        hostPort.substringAfter("]:").toIntOrNull() ?: -1
                    !hostPort.startsWith("[") && ':' in hostPort ->
                        hostPort.substringAfter(':').toIntOrNull() ?: -1
                    else -> -1
                }

            return AmqpUrlParts(
                user = user,
                password = password,
                host = host,
                port = port,
                vhostPath = vhostPath,
            )
        }

        private data class AmqpUrlParts(
            val user: String,
            val password: String,
            val host: String,
            val port: Int,
            val vhostPath: String,
        )
    }
}
