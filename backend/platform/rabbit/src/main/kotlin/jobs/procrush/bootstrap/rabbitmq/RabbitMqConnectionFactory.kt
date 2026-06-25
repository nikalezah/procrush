package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import java.net.URI

internal object RabbitMqConnectionFactory {
    fun configure(factory: ConnectionFactory, url: String) {
        factory.setUri(url)
        val vhostFromUri = URI(url).path?.removePrefix("/").orEmpty()
        if (factory.virtualHost.isNullOrEmpty() && vhostFromUri.isEmpty()) {
            factory.virtualHost = "/"
        }
    }
}
