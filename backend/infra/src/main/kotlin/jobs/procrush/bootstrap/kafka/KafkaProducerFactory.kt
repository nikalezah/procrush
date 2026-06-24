package jobs.procrush.bootstrap.kafka

import jobs.procrush.bootstrap.config.KafkaConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

object KafkaProducerFactory {
    fun create(config: KafkaConfig): KafkaProducer<String, String> {
        val props =
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.ACKS_CONFIG, "all")
                put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                put(ProducerConfig.RETRIES_CONFIG, "3")
            }
        return KafkaProducer(props)
    }
}
