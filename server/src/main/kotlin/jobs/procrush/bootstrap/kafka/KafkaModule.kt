package jobs.procrush.bootstrap.kafka

import jobs.procrush.bootstrap.config.KafkaConfig
import org.apache.kafka.clients.producer.KafkaProducer

class KafkaModule private constructor(
    val producer: KafkaProducer<String, String>,
    val config: KafkaConfig,
) {
    fun close() {
        runCatching { producer.close() }
    }

    companion object {
        fun create(config: KafkaConfig): KafkaModule {
            val producer = KafkaProducerFactory.create(config)
            return KafkaModule(producer, config)
        }
    }
}
