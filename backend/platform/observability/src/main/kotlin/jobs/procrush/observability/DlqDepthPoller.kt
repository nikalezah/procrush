package jobs.procrush.observability

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

class DlqDepthPoller(
    private val rabbitMqUrl: String,
    private val queueName: String,
    private val intervalSeconds: Long = 30,
) {
    private val logger = LoggerFactory.getLogger(DlqDepthPoller::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()

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
    }

    private fun pollOnce() {
        val managementUri = toManagementQueueUri(rabbitMqUrl, queueName)
        val credentials = parseCredentials(rabbitMqUrl)
        val auth = Base64.getEncoder().encodeToString("${credentials.first}:${credentials.second}".toByteArray())
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(managementUri))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic $auth")
                .GET()
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return
        val messagesRegex = """"messages"\s*:\s*(\d+)""".toRegex()
        val depth = messagesRegex.find(response.body())?.groupValues?.get(1)?.toDoubleOrNull() ?: return
        AppMetrics.setRabbitMqQueueDepth(queueName, depth)
    }

    companion object {
        private fun toManagementQueueUri(
            amqpUrl: String,
            queueName: String,
        ): String {
            val uri = URI(amqpUrl.replace("amqp://", "http://"))
            val host = uri.host ?: "localhost"
            val port = if (uri.port > 0) uri.port + 10000 else 15672
            val vhost = uri.path.takeIf { it.isNotBlank() && it != "/" } ?: "%2F"
            val encodedQueue = queueName.replace("/", "%2F")
            return "http://$host:$port/api/queues/$vhost/$encodedQueue"
        }

        private fun parseCredentials(amqpUrl: String): Pair<String, String> {
            val uri = URI(amqpUrl)
            val userInfo = uri.userInfo?.split(":") ?: return "guest" to "guest"
            val user = userInfo.getOrElse(0) { "guest" }
            val pass = userInfo.getOrElse(1) { "guest" }
            return user to pass
        }
    }
}
